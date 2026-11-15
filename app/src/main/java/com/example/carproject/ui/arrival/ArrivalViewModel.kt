package com.example.carproject.ui.arrival

import com.example.carproject.data.AppClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.carproject.AppContainer
import com.example.carproject.data.model.Seat
import com.example.carproject.data.model.VehicleStatus
import com.example.carproject.data.repository.CalendarRepository
import com.example.carproject.data.repository.PlaceRepository
import com.example.carproject.data.repository.VehicleRepository
import com.example.carproject.data.repository.VoiceRepository
import com.example.carproject.driving.DrivingAssistant
import com.example.carproject.ui.briefing.BriefingViewModel
import com.example.carproject.ui.components.AppIcon
import com.example.carproject.ui.home.CheckItem
import com.example.carproject.ui.theme.CommuteMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalTime

/** 내리기 전에 처리할 것 하나. 지금은 열린 창문만 실행까지 한다. */
sealed interface ArrivalIssue {
    data class WindowOpen(val seat: Seat) : ArrivalIssue
}

data class ArrivalUiState(
    val mode: CommuteMode,
    val statusText: String,
    val title: String,
    val subtitle: String,
    val issue: ArrivalIssue?,
    val closing: Boolean,
    val checks: List<CheckItem>,
    val tripLabel: String,
    val tripSummary: String,
)

/** design/09_ArrivalWork.html(출근·라이트), 10_Arrival.html(퇴근·다크): 내리기 전 확인. */
class ArrivalViewModel(
    private val vehicle: VehicleRepository,
    private val places: PlaceRepository,
    private val calendar: CalendarRepository,
    private val voice: VoiceRepository,
    private val driving: DrivingAssistant,
) : ViewModel() {
    private val closing = MutableStateFlow(false)
    private val mode = driving.mode.value

    val uiState: StateFlow<ArrivalUiState> = combine(vehicle.status, closing) { v, c -> build(v, c) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), build(vehicle.status.value, false))

    private var spoken = false

    /** 화면에 들어오면 한 번, 물어볼 것이나 결과를 읽어 준다. */
    fun onEnter() {
        if (spoken) return
        spoken = true
        val s = uiState.value
        viewModelScope.launch { voice.speak("${s.title.replace("\n", " ")} ${s.subtitle}") }
    }

    /** "창문 닫기". 화면에서 물은 뒤 사용자가 눌렀을 때만 실행한다. */
    fun onFix() {
        val issue = uiState.value.issue as? ArrivalIssue.WindowOpen ?: return
        if (closing.value) return
        voice.stop()
        closing.value = true
        viewModelScope.launch {
            val ok = vehicle.closeWindow(issue.seat)
            closing.value = false
            if (ok) voice.speak("${issue.seat.label} 창문을 닫았어요.")
        }
    }

    fun stop() = voice.stop()

    override fun onCleared() = stop()

    private fun build(v: VehicleStatus, closingNow: Boolean): ArrivalUiState {
        val evening = mode == CommuteMode.Evening
        val place = driving.destination ?: if (evening) places.home() else places.workplace()
        val placeLabel = when (place) {
            places.workplace() -> "회사"
            places.home() -> "집"
            else -> place.name
        }
        val openWindow = v.openWindows.firstOrNull()
        val issue = openWindow?.let { ArrivalIssue.WindowOpen(it) }

        val checks = listOf(
            if (openWindow != null) CheckItem(AppIcon.Window, "창문", "${openWindow.label} 열림", warning = true)
            else CheckItem(AppIcon.Window, "창문", "모두 닫힘"),
            if (v.openDoors.isNotEmpty()) CheckItem(AppIcon.Lock, "문", "${v.openDoors.first().label} 열림", warning = true)
            else CheckItem(AppIcon.Lock, "문", "모두 닫힘"),
            if (v.lightsOn) CheckItem(AppIcon.Bulb, "조명", "켜져 있음", warning = true)
            else CheckItem(AppIcon.Bulb, "조명", "모두 꺼짐"),
            CheckItem(AppIcon.Chat, "Slack 상태", "\"이동 중\" 해제"),
        )

        val title = when {
            closingNow -> "창문을\n닫고 있어요."
            openWindow != null -> "${openWindow.label} 창문이\n열려 있어요. 닫을까요?"
            evening -> "모두 확인했어요.\n편히 쉬세요."
            else -> "모두 확인했어요.\n잘 다녀오세요."
        }
        val subtitle = buildString {
            append(if (openWindow != null) "나머지는 모두 확인했어요. " else "창문, 문, 조명 모두 괜찮아요. ")
            append(if (evening) "오늘도 수고하셨어요." else nextEventText() ?: "오늘도 좋은 하루 보내세요.")
        }
        return ArrivalUiState(
            mode = mode,
            statusText = "주차 중 · $placeLabel 도착",
            title = title,
            subtitle = subtitle,
            issue = issue,
            closing = closingNow,
            checks = checks,
            tripLabel = if (evening) "오늘 퇴근길" else "오늘 출근길",
            tripSummary = driving.tripSummary(),
        )
    }

    /** "9시 주간 회의까지 12분 남았어요." */
    private fun nextEventText(): String? {
        val now = AppClock.now()
        val next = calendar.todayEvents().sortedBy { it.time }.firstOrNull { it.time.isAfter(now) } ?: return null
        val minutes = Duration.between(now, next.time).toMinutes()
        return "${BriefingViewModel.koreanTime(next.time)} ${next.title}까지 ${minutes}분 남았어요."
    }

    companion object {
        fun factory(c: AppContainer) = viewModelFactory {
            initializer {
                ArrivalViewModel(c.vehicleRepository, c.placeRepository, c.calendarRepository, c.voiceRepository, c.drivingAssistant)
            }
        }
    }
}
