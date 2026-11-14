package com.example.carproject.driving

import android.util.Log
import com.example.carproject.data.model.Place
import com.example.carproject.data.model.SpeechEvent
import com.example.carproject.data.repository.CalendarRepository
import com.example.carproject.data.repository.MessageRepository
import com.example.carproject.data.repository.SpeechRepository
import com.example.carproject.data.repository.VoiceRepository
import com.example.carproject.ui.theme.CommuteMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime

/** 주행 중 내비 위에 띄우는 작은 팝업. */
sealed interface DrivingPopup {
    data object None : DrivingPopup

    /** design/05_LateNotice.html: 늦을 것 같으니 팀에 알릴지 묻는다. */
    data class LateNotice(
        val lateMinutes: Int,
        val channel: String,
        val message: String,
        val arrivalText: String,
    ) : DrivingPopup

    /** design/06_DrivingCalm.html: 보냈다고 짧게 알린다. */
    data class Sent(val arrivalText: String) : DrivingPopup
}

/**
 * 주행 중 판단을 맡는 앱 전체 상태 관리자. 내비 위 오버레이에는 ViewModel을 붙일 수 없어서
 * Application 수명으로 두고, OverlayPopupHost가 [popup]을 보고 창을 띄운다.
 * 무엇이든 보내기 전에는 항상 묻는다(CLAUDE.md).
 */
class DrivingAssistant(
    private val scope: CoroutineScope,
    private val calendar: CalendarRepository,
    private val messages: MessageRepository,
    private val speech: SpeechRepository,
    private val voice: VoiceRepository,
) {
    private val _popup = MutableStateFlow<DrivingPopup>(DrivingPopup.None)
    val popup: StateFlow<DrivingPopup> = _popup.asStateFlow()

    private val _mode = MutableStateFlow(CommuteMode.Morning)

    /** 출근길(라이트)인지 퇴근길(다크)인지. 주차·도착 화면과 주행 팝업의 테마를 정한다. */
    val mode: StateFlow<CommuteMode> = _mode.asStateFlow()

    fun setMode(mode: CommuteMode) {
        Log.i(TAG, "모드: $mode")
        // 출근↔퇴근이 바뀌면 이전 길의 목적지는 더 이상 맞지 않는다(퇴근인데 "회사 도착"이 뜨지 않게).
        if (_mode.value != mode) destination = null
        _mode.value = mode
    }

    /** 오늘 이번 길의 거리·시간. 실제 운행 기록이 없어서 가짜 값이다. */
    fun tripSummary(): String = if (_mode.value == CommuteMode.Evening) "18.6km · 44분" else "18.4km · 41분"

    /** 지금 가고 있는(또는 마지막으로 간) 목적지. 주차 위치를 물을 때 장소 이름으로 쓴다. */
    var destination: Place? = null
        private set

    private var driveJob: Job? = null
    private var popupJob: Job? = null

    /** 길안내가 시작됐다. 가짜 교통 정보로 잠시 뒤 지각 여부를 판단한다. */
    fun onRouteStarted(place: Place) {
        Log.i(TAG, "주행 시작: ${place.name}")
        destination = place
        driveJob?.cancel()
        driveJob = scope.launch {
            delay(LATE_CHECK_DELAY_MS)
            showLateNotice(FAKE_DELAY_MINUTES)
        }
    }

    /** 시연용: 지각 알림을 바로 띄운다. */
    fun showLateNotice(lateMinutes: Int = FAKE_DELAY_MINUTES) {
        val next = calendar.todayEvents().sortedBy { it.time }.firstOrNull { it.time.isAfter(LocalTime.now()) }
            ?: calendar.todayEvents().first()
        val arrival = next.time.plusMinutes(lateMinutes.toLong())
        val notice = DrivingPopup.LateNotice(
            lateMinutes = lateMinutes,
            channel = messages.teamChannel(),
            message = "출근길 정체로 ${lateMinutes}분 정도 늦습니다",
            arrivalText = "%d:%02d".format(arrival.hour, arrival.minute),
        )
        _popup.value = notice
        popupJob?.cancel()
        popupJob = scope.launch {
            // 운전 중에는 화면을 못 보니 질문을 읽어 준 뒤, 말로도 답을 받는다. 답이 없으면 보내지 않고 닫는다.
            val listen = launch {
                voice.speak("${notice.lateMinutes}분 늦을 것 같아요. 팀에 알릴까요?")
                listenForAnswer()
            }
            delay(NO_ANSWER_CLOSE_MS)
            listen.cancel()
            if (_popup.value == notice) _popup.value = DrivingPopup.None
        }
    }

    /** "보내기" */
    fun onSend() {
        val notice = _popup.value as? DrivingPopup.LateNotice ?: return
        popupJob?.cancel()
        voice.stop()
        popupJob = scope.launch {
            val ok = messages.sendToTeam(notice.message)
            if (!ok) {
                _popup.value = DrivingPopup.None
                return@launch
            }
            _popup.value = DrivingPopup.Sent(notice.arrivalText)
            delay(SENT_SHOW_MS)
            _popup.value = DrivingPopup.None
        }
    }

    /** "괜찮아요" 또는 닫기 */
    fun onDismiss() {
        popupJob?.cancel()
        voice.stop()
        _popup.value = DrivingPopup.None
    }

    fun stopDriving() {
        driveJob?.cancel()
        onDismiss()
    }

    /** 팝업이 떠 있는 동안 계속 듣는다. 말이 없어 끊기면 다시 듣는다. */
    private suspend fun listenForAnswer() {
        while (true) {
            speech.listen().collect { event ->
                if (event !is SpeechEvent.Final) return@collect
                val text = event.text.replace(" ", "")
                when {
                    "보내" in text -> onSend()
                    listOf("괜찮", "아니", "됐어", "하지마").any { it in text } -> onDismiss()
                }
            }
            delay(300)
        }
    }

    private companion object {
        const val TAG = "DrivingAssistant"
        const val FAKE_DELAY_MINUTES = 8
        const val LATE_CHECK_DELAY_MS = 6_000L
        const val NO_ANSWER_CLOSE_MS = 20_000L
        const val SENT_SHOW_MS = 4_000L
    }
}
