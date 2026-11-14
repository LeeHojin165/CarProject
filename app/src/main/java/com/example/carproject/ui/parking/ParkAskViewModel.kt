package com.example.carproject.ui.parking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.carproject.AppContainer
import com.example.carproject.data.model.ParkingSpot
import com.example.carproject.data.model.SpeechEvent
import com.example.carproject.data.repository.ParkingRepository
import com.example.carproject.data.repository.SpeechRepository
import com.example.carproject.data.repository.VoiceRepository
import com.example.carproject.ui.components.VoiceInput
import com.example.carproject.ui.navigation.PlaceArgs
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime

data class ParkAskUiState(
    val placeName: String,
    val placeLabel: String,
    val parkedTimeText: String,
    val last: ParkingSpot?,
    val selectedFloor: String? = null,
    val voice: VoiceInput = VoiceInput(),
)

/** design/07_ParkAsk.html: P로 바꾸면 주차 위치를 묻는다. */
class ParkAskViewModel(
    savedState: SavedStateHandle,
    private val parking: ParkingRepository,
    private val speech: SpeechRepository,
    private val voice: VoiceRepository,
) : ViewModel() {
    private val place = PlaceArgs.read(savedState)
    private val parkedAt = LocalTime.now().withSecond(0).withNano(0)

    private val _uiState = MutableStateFlow(
        ParkAskUiState(
            placeName = place.name,
            placeLabel = "${place.name} 주차장",
            parkedTimeText = "%02d:%02d".format(parkedAt.hour, parkedAt.minute),
            last = parking.lastAt(place.name),
        )
    )
    val uiState: StateFlow<ParkAskUiState> = _uiState.asStateFlow()

    private val savedChannel = Channel<ParkingSpot>(Channel.BUFFERED)

    /** 저장되면 한 번 흘러나온다. */
    val saved: Flow<ParkingSpot> = savedChannel.receiveAsFlow()

    private var listenJob: Job? = null
    private var askedOnce = false

    /** 화면에 들어올 때마다 부른다. 처음에만 질문을 읽어 준다. */
    fun onEnter() {
        listenJob?.cancel()
        listenJob = viewModelScope.launch {
            if (!askedOnce) {
                askedOnce = true
                voice.speak("주차했어요. 어디에 세웠나요?")
            }
            listen()
        }
    }

    fun onRetryVoice() {
        listenJob?.cancel()
        listenJob = viewModelScope.launch { listen() }
    }

    fun onFloorClick(floor: String) {
        _uiState.update { it.copy(selectedFloor = if (it.selectedFloor == floor) null else floor) }
    }

    /** "지난번과 같아요" */
    fun onSameAsLast() {
        val last = _uiState.value.last ?: return
        save(last.copy(parkedAt = parkedAt))
    }

    fun stop() {
        listenJob?.cancel()
        voice.stop()
    }

    private suspend fun listen() {
        updateVoice { VoiceInput(listening = true) }
        speech.listen().collect { event ->
            when (event) {
                SpeechEvent.Listening -> updateVoice { it.copy(listening = true, error = null) }
                is SpeechEvent.Partial -> updateVoice { it.copy(listening = true, transcript = event.text) }
                is SpeechEvent.Failed -> {
                    // 층만 고르고 말이 없었다면 층만이라도 저장한다.
                    val floor = _uiState.value.selectedFloor
                    if (floor != null) save(ParkingSpot(place.name, floor, null, parkedAt))
                    else updateVoice { VoiceInput(error = event.reason) }
                }
                is SpeechEvent.Final -> {
                    updateVoice { VoiceInput(transcript = event.text, confirmed = true) }
                    delay(CONFIRM_DELAY_MS)
                    save(parse(event.text))
                }
            }
        }
        updateVoice { if (it.listening) it.copy(listening = false) else it }
    }

    private fun save(spot: ParkingSpot) {
        listenJob?.cancel()
        viewModelScope.launch {
            parking.save(spot)
            parking.sendToPhone(spot)
            savedChannel.send(spot)
        }
    }

    private fun updateVoice(transform: (VoiceInput) -> VoiceInput) =
        _uiState.update { it.copy(voice = transform(it.voice)) }

    /** "지하 3층 기둥 C12", "B3 씨 12" 같은 말에서 층과 기둥 번호를 뽑는다. */
    private fun parse(text: String): ParkingSpot {
        val t = text.replace(" ", "")
        val floor = Regex("지하(\\d+)").find(t)?.let { "B${it.groupValues[1]}" }
            ?: Regex("[Bb비](\\d+)").find(t)?.let { "B${it.groupValues[1]}" }
            ?: if ("지상" in t) "지상" else null
        val letters = mapOf("에이" to "A", "비" to "B", "씨" to "C", "시" to "C", "디" to "D", "이" to "E", "에프" to "F")
        val afterPillar = t.substringAfter("기둥", missingDelimiterValue = "")
        val spot = Regex("^([A-Za-z]|에이|비|씨|시|디|이|에프)?-?(\\d{1,3})").find(afterPillar)?.let { m ->
            val letter = m.groupValues[1].let { letters[it] ?: it.uppercase() }
            "$letter${m.groupValues[2]}"
        } ?: Regex("([A-Z])-?(\\d{1,3})").findAll(text.uppercase()).lastOrNull()
            ?.takeIf { "B${it.groupValues[2]}" != floor }
            ?.let { "${it.groupValues[1]}${it.groupValues[2]}" }
        return ParkingSpot(
            placeName = place.name,
            floor = floor ?: _uiState.value.selectedFloor,
            spot = spot,
            parkedAt = parkedAt,
            note = text.takeIf { floor == null && spot == null },
        )
    }

    override fun onCleared() = stop()

    companion object {
        private const val CONFIRM_DELAY_MS = 1_000L

        fun factory(c: AppContainer) = viewModelFactory {
            initializer {
                ParkAskViewModel(createSavedStateHandle(), c.parkingRepository, c.speechRepository, c.voiceRepository)
            }
        }
    }
}
