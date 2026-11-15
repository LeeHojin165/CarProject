package com.example.carproject.ui.home

import com.example.carproject.data.AppClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.carproject.AppContainer
import com.example.carproject.data.model.CalendarEvent
import com.example.carproject.data.model.Place
import com.example.carproject.data.model.RecentPlace
import com.example.carproject.data.model.SpeechEvent
import com.example.carproject.data.model.VehicleStatus
import com.example.carproject.data.repository.CalendarRepository
import com.example.carproject.data.repository.PlaceRepository
import com.example.carproject.data.repository.SettingsRepository
import com.example.carproject.data.repository.SpeechRepository
import com.example.carproject.data.repository.VoiceRepository
import com.example.carproject.data.repository.VehicleRepository
import com.example.carproject.ui.components.AppIcon
import com.example.carproject.ui.components.VoiceInput
import com.example.carproject.driving.DrivingAssistant
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

data class CheckItem(val icon: AppIcon, val label: String, val value: String, val warning: Boolean = false)

/** 첫 화면 위에 띄우는 대화상자. */
sealed interface HomeDialog {
    data object None : HomeDialog

    /** design/02_DestConfirm.html: 회사가 맞는지 묻기 */
    data class ConfirmWork(val workplace: Place, val arrivalText: String) : HomeDialog

    /** design/18_TripDest.html: 출장지 묻기. 오늘 일정의 회사 밖 미팅을 먼저 제안한다. */
    data class AskTrip(
        val suggestion: TripSuggestion?,
        val recentPlaces: List<RecentPlace>,
        val voice: VoiceInput = VoiceInput(),
    ) : HomeDialog

    /** design/03_DestAsk.html: 다른 곳이면 어디로 갈지 묻기 */
    data class AskDestination(
        val recentPlaces: List<RecentPlace>,
        val voice: VoiceInput = VoiceInput(),
    ) : HomeDialog
}

/** 18에서 제안하는 오늘 일정의 출장지. */
data class TripSuggestion(val event: CalendarEvent, val place: Place, val arrivalText: String)

/** 첫 화면에서 정해진 다음 장면. */
sealed interface HomeNext {
    data class Commute(val place: Place) : HomeNext
    data class Business(val place: Place) : HomeNext
}

data class HomeUiState(
    val hint: String = "",
    val checks: List<CheckItem> = emptyList(),
    val footnote: String = "",
    val dialog: HomeDialog = HomeDialog.None,
)

class HomeViewModel(
    private val vehicle: VehicleRepository,
    private val places: PlaceRepository,
    private val speech: SpeechRepository,
    private val voice: VoiceRepository,
    private val driving: DrivingAssistant,
    private val calendar: CalendarRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    private var speakJob: Job? = null
    private val dialog = MutableStateFlow<HomeDialog>(HomeDialog.None)
    private var listenJob: Job? = null

    private val destinationChannel = Channel<HomeNext>(Channel.BUFFERED)

    /** 목적지가 정해졌을 때 한 번 흘러나온다. 화면이 받아서 다음 단계로 넘어간다. */
    val destinationChosen: Flow<HomeNext> = destinationChannel.receiveAsFlow()

    val uiState: StateFlow<HomeUiState> = combine(vehicle.status, dialog) { v, d -> toUiState(v, d) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            toUiState(vehicle.status.value, HomeDialog.None),
        )

    /** "네, 출근해요" → 회사가 맞는지 묻는다. */
    fun onCommuteClick() {
        driving.beginCommute()
        // CLAUDE.md: 출근 버튼을 누르면 차량 상태를 한 문장으로 읽어준다.
        speakJob?.cancel()
        if (settings.proactive.value.vehicleCheck) {
            speakJob = viewModelScope.launch { voice.speak(vehicleSentence(vehicle.status.value)) }
        }
        showConfirmWork()
    }

    /** 회사가 맞는지 묻는 대화상자. "회사로 갈게요"로 돌아올 때는 다시 읽지 않는다. */
    fun showConfirmWork() {
        stopListening()
        val work = places.workplace()
        val arrival = AppClock.now().plusMinutes(places.estimatedMinutes(work).toLong())
        dialog.value = HomeDialog.ConfirmWork(work, "${arrival.hour}시 ${arrival.minute}분")
    }

    /** "다른 곳이에요" → 어디로 갈지 묻는다. */
    fun onOtherPlaceClick() {
        dialog.value = HomeDialog.AskDestination(places.recentPlaces())
        startListening()
    }

    /** 음성으로 목적지를 받는다. 음성 상자를 다시 누르면 처음부터 다시 듣는다. */
    fun startListening() {
        // 마이크가 TTS 소리를 듣지 않게 읽던 것을 멈춘다.
        stopSpeaking()
        listenJob?.cancel()
        updateVoice { VoiceInput(listening = true) }
        listenJob = viewModelScope.launch {
            speech.listen().collect { event ->
                when (event) {
                    SpeechEvent.Listening -> updateVoice { it.copy(listening = true, error = null) }
                    is SpeechEvent.Partial -> updateVoice { it.copy(listening = true, transcript = event.text) }
                    is SpeechEvent.Failed -> updateVoice { VoiceInput(error = event.reason) }
                    is SpeechEvent.Final -> {
                        updateVoice { VoiceInput(transcript = event.text, confirmed = true) }
                        // 무엇을 알아들었는지 잠깐 보여준 뒤 넘어간다.
                        delay(CONFIRM_DELAY_MS)
                        if (dialog.value is HomeDialog.AskTrip) chooseTrip(resolvePlace(event.text, places.recentTripPlaces()))
                        else chooseDestination(resolvePlace(event.text, places.recentPlaces()))
                    }
                }
            }
            updateVoice { if (it.listening) it.copy(listening = false) else it }
        }
    }

    fun chooseDestination(place: Place) {
        stopListening()
        dialog.value = HomeDialog.None
        destinationChannel.trySend(HomeNext.Commute(place))
    }

    /** "출장 가요" → 어디로 가는지 묻는다(18). 일정에 회사 밖 미팅이 있으면 먼저 제안한다. */
    fun onBusinessTripClick() {
        stopSpeaking()
        val now = AppClock.now()
        val event = calendar.todayEvents().sortedBy { it.time }.firstOrNull { it.outside && it.place != null && it.time.isAfter(now) }
            ?: calendar.todayEvents().firstOrNull { it.outside && it.place != null }
        val suggestion = event?.let { e ->
            val arrival = now.plusMinutes(places.estimatedMinutes(e.place!!).toLong())
            TripSuggestion(e, e.place, "${arrival.hour}시 %02d분".format(arrival.minute))
        }
        dialog.value = HomeDialog.AskTrip(suggestion, places.recentTripPlaces())
        startListening()
    }

    fun chooseTrip(place: Place) {
        stopListening()
        dialog.value = HomeDialog.None
        destinationChannel.trySend(HomeNext.Business(place))
    }

    /** "퇴근이에요": 퇴근 모드로 바꾸고 11로 넘어간다. */
    fun onLeaveWorkClick() {
        stopSpeaking()
        driving.beginLeaveWork()
    }

    fun dismissDialog() {
        stopListening()
        dialog.update { HomeDialog.None }
    }

    private fun stopSpeaking() {
        speakJob?.cancel()
        speakJob = null
        voice.stop()
    }

    /** 차량 상태를 한 문장으로. TTS가 읽기 쉽게 단위를 한글로 쓴다. */
    private fun vehicleSentence(v: VehicleStatus): String {
        val energy = v.evBatteryPercent?.let { "배터리 ${it}퍼센트로 ${v.rangeKm}킬로미터 갈 수 있고" }
            ?: "연료 ${v.fuelPercent ?: 0}퍼센트로 ${v.rangeKm}킬로미터 갈 수 있고"
        val tire = if (v.tires.isNormal()) "타이어는 네 바퀴 모두 정상이에요" else "타이어 공기압은 점검이 필요해요"
        val warnings = v.warningLights.size + if (v.brakeWarning) 1 else 0
        val extra = buildList {
            if (warnings > 0) add("경고등이 ${warnings}개 켜져 있어요")
            if (v.washerFluidLow) add("워셔액은 보충이 필요해요")
            if (v.openDoors.isNotEmpty()) add("${v.openDoors.first().label} 문이 열려 있어요")
        }
        return (listOf("출발 전에 살펴봤어요. $energy, $tire.") + extra.map { "$it." }).joinToString(" ")
    }

    override fun onCleared() {
        voice.stop()
    }

    private fun stopListening() {
        listenJob?.cancel()
        listenJob = null
    }

    private fun updateVoice(transform: (VoiceInput) -> VoiceInput) = dialog.update { d ->
        when (d) {
            is HomeDialog.AskDestination -> d.copy(voice = transform(d.voice))
            is HomeDialog.AskTrip -> d.copy(voice = transform(d.voice))
            else -> d
        }
    }

    /** 말한 문장에서 목적지를 찾는다. 최근에 간 곳 이름이 들어 있으면 그곳, 아니면 말한 곳 그대로. */
    private fun resolvePlace(text: String, recent: List<RecentPlace>): Place {
        val bare = { s: String -> s.replace("[", "").replace("]", "").replace(" ", "") }
        recent.firstOrNull { bare(text).contains(bare(it.place.name)) }?.let { return it.place }
        val name = text.trim()
            .removeSuffix("가줘").removeSuffix("가 줘").removeSuffix("안내해줘").trim()
            .removeSuffix("으로").removeSuffix("로").trim()
        return Place(name = name.ifBlank { text }, address = "")
    }

    private fun toUiState(v: VehicleStatus, d: HomeDialog): HomeUiState {
        // 촬영 날짜가 주말이어도 출근 장면이 어색하지 않게, 주말이면 평일로 말한다.
        val today = LocalDate.now().dayOfWeek
        val weekday = if (today.value >= 6) "평일" else today.getDisplayName(TextStyle.FULL, Locale.KOREAN)
        val energy = when {
            v.evBatteryPercent != null -> CheckItem(AppIcon.Battery, "배터리", "${v.evBatteryPercent}% · ${v.rangeKm}km")
            else -> CheckItem(AppIcon.Drop, "연료", "${v.fuelPercent ?: 0}% · ${v.rangeKm}km")
        }
        val tireOk = v.tires.isNormal()
        val warnings = v.warningLights.size + if (v.brakeWarning) 1 else 0
        return HomeUiState(
            hint = "${weekday} 이 시간에는 보통 회사로 출발하셨어요.",
            checks = listOf(
                energy,
                CheckItem(AppIcon.Tire, "타이어 공기압", if (tireOk) "네 바퀴 정상" else "점검 필요", !tireOk),
                CheckItem(AppIcon.Warning, "경고등", if (warnings == 0) "없음" else "${warnings}개 켜짐", warnings > 0),
                CheckItem(AppIcon.Drop, "워셔액", if (v.washerFluidLow) "부족 · 보충 필요" else "충분", v.washerFluidLow),
            ),
            footnote = if (v.washerFluidLow) "가는 길 주유소에서 넣을 수 있게, 출발하면 다시 알려드릴게요."
            else "모두 괜찮아요. 바로 출발하셔도 돼요.",
            dialog = d,
        )
    }

    companion object {
        fun factory(c: AppContainer) = viewModelFactory {
            initializer { HomeViewModel(c.vehicleRepository, c.placeRepository, c.speechRepository, c.voiceRepository, c.drivingAssistant, c.calendarRepository, c.settingsRepository) }
        }

        private const val CONFIRM_DELAY_MS = 1_200L
    }
}
