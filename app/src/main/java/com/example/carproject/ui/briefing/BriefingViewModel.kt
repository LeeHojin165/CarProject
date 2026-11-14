package com.example.carproject.ui.briefing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.carproject.AppContainer
import com.example.carproject.data.model.Mention
import com.example.carproject.data.model.Place
import com.example.carproject.data.repository.CalendarRepository
import com.example.carproject.data.repository.MessageRepository
import com.example.carproject.data.repository.NavigationRepository
import com.example.carproject.data.repository.RouteResult
import com.example.carproject.data.repository.VoiceRepository
import com.example.carproject.driving.DrivingAssistant
import com.example.carproject.data.repository.PlaceRepository
import com.example.carproject.ui.components.withRo
import com.example.carproject.ui.navigation.PlaceArgs
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalTime

data class EventItem(val timeText: String, val title: String, val location: String?, val outside: Boolean)

/** 다음 일정까지 여유(양수) 또는 늦는 시간(음수). */
data class NextEventGap(val label: String, val minutes: Long)

data class BriefingUiState(
    val place: Place,
    /** 제목에 쓰는 이름. 직장이면 "회사". */
    val placeLabel: String,
    val travelMinutes: Int,
    val arrivalText: String,
    val nextEvent: NextEventGap?,
    val destinationSentence: String,
    val events: List<EventItem>,
    val mentions: List<Mention>,
    /** 멘션 메시지를 읽고 있다. */
    val readingMentions: Boolean = false,
    /** 출발 버튼을 누른 뒤 상태. */
    val departure: DepartureState = DepartureState.Idle,
)

sealed interface DepartureState {
    data object Idle : DepartureState
    data object Requesting : DepartureState
    data class Failed(val reason: String) : DepartureState
}

/** design/04_Briefing.html: 목적지가 정해진 뒤 출근 브리핑. */
class BriefingViewModel(
    savedState: SavedStateHandle,
    places: PlaceRepository,
    calendar: CalendarRepository,
    messages: MessageRepository,
    private val navigation: NavigationRepository,
    private val driving: DrivingAssistant,
    private val voice: VoiceRepository,
) : ViewModel() {
    private var readJob: Job? = null
    private val place = PlaceArgs.read(savedState)

    private val _uiState = MutableStateFlow(build(places, calendar, messages))
    val uiState: StateFlow<BriefingUiState> = _uiState.asStateFlow()

    private val routeStarted = Channel<RouteResult>(Channel.BUFFERED)

    /** 길안내가 시작되면 한 번 흘러나온다. */
    val departed: Flow<RouteResult> = routeStarted.receiveAsFlow()

    /** "멘션 메시지 읽어줘". 읽는 중에 다시 누르면 멈춘다. */
    fun onReadMentions() {
        if (readJob?.isActive == true) {
            stopReading()
            return
        }
        val mentions = _uiState.value.mentions
        val text = if (mentions.isEmpty()) "나를 언급한 메시지가 없어요." else buildString {
            append("나를 언급한 메시지가 ${mentions.size}개 있어요. ")
            mentions.forEach { m -> append("${m.sender}님이 ${m.channel} 채널에서, ${m.text} 라고 했어요. ") }
        }.forSpeech()
        readJob = viewModelScope.launch {
            _uiState.update { it.copy(readingMentions = true) }
            try {
                voice.speak(text)
            } finally {
                _uiState.update { it.copy(readingMentions = false) }
            }
        }
    }

    private fun stopReading() {
        readJob?.cancel()
        voice.stop()
    }

    /** 자리표시 괄호나 #은 소리 내어 읽지 않는다. */
    private fun String.forSpeech() = replace("[", "").replace("]", "").replace("#", "")

    override fun onCleared() = stopReading()

    /** "출발할게요": 내비게이션으로 길안내를 시작한다. */
    fun onDepart() {
        stopReading()
        if (_uiState.value.departure == DepartureState.Requesting) return
        _uiState.update { it.copy(departure = DepartureState.Requesting) }
        viewModelScope.launch {
            when (val result = navigation.startRoute(place)) {
                is RouteResult.Failed -> _uiState.update { it.copy(departure = DepartureState.Failed(result.reason)) }
                else -> {
                    _uiState.update { it.copy(departure = DepartureState.Idle) }
                    driving.onRouteStarted(place)
                    routeStarted.send(result)
                }
            }
        }
    }

    private fun build(places: PlaceRepository, calendar: CalendarRepository, messages: MessageRepository): BriefingUiState {
        val isWork = place == places.workplace()
        val label = if (isWork) "회사" else place.name
        val minutes = places.estimatedMinutes(place)
        val arrival = LocalTime.now().plusMinutes(minutes.toLong())
        val events = calendar.todayEvents().sortedBy { it.time }
        // 도착 전후로 가장 가까운 다음 일정. 이미 지난 일정은 건너뛴다.
        val next = events.firstOrNull { it.time.isAfter(LocalTime.now()) }
        return BriefingUiState(
            place = place,
            placeLabel = label,
            travelMinutes = minutes,
            arrivalText = koreanTime(arrival),
            nextEvent = next?.let {
                NextEventGap("${koreanTime(it.time)} ${it.title}", Duration.between(arrival, it.time).toMinutes())
            },
            destinationSentence = "목적지는 ${withRo(label)} 맞춰 뒀어요.",
            events = events.map {
                EventItem("%d:%02d".format(it.time.hour, it.time.minute), it.title, it.location, it.outside)
            },
            mentions = messages.unreadMentions(),
        )
    }

    companion object {
        /** "8시 46분", 정각이면 "9시". */
        fun koreanTime(t: LocalTime) = if (t.minute == 0) "${t.hour}시" else "${t.hour}시 ${t.minute}분"

        fun factory(c: AppContainer) = viewModelFactory {
            initializer {
                BriefingViewModel(
                    createSavedStateHandle(), c.placeRepository, c.calendarRepository,
                    c.messageRepository, c.navigationRepository, c.drivingAssistant, c.voiceRepository,
                )
            }
        }
    }
}
