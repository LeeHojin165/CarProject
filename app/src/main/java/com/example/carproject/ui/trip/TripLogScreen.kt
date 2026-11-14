package com.example.carproject.ui.trip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.carproject.AppContainer
import com.example.carproject.data.model.SpeechEvent
import com.example.carproject.data.model.TripLogEntry
import com.example.carproject.data.repository.ProfileRepository
import com.example.carproject.data.repository.SpeechRepository
import com.example.carproject.data.repository.TripLogRepository
import com.example.carproject.data.repository.VehicleRepository
import com.example.carproject.data.repository.VoiceRepository
import com.example.carproject.driving.DrivingAssistant
import com.example.carproject.ui.components.AppButton
import com.example.carproject.ui.components.AppIcon
import com.example.carproject.ui.components.AppScreen
import com.example.carproject.ui.components.ButtonKind
import com.example.carproject.ui.components.VoiceInput
import com.example.carproject.ui.components.VoicePromptBox
import com.example.carproject.ui.components.withIGa
import com.example.carproject.ui.theme.appColors
import com.example.carproject.ui.theme.ds
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

data class TripLogUiState(
    val entry: TripLogEntry,
    val fuelFormula: String,
    val purposeConfirmed: Boolean = false,
    /** 목적을 말로 바꾸는 중이면 음성 상자를 보여준다. */
    val editing: VoiceInput? = null,
)

/** design/20_TripLog.html: 출장지에 도착하면 운행기록과 유류비를 채우고 목적만 확인받는다. */
class TripLogViewModel(
    driving: DrivingAssistant,
    vehicle: VehicleRepository,
    profile: ProfileRepository,
    private val tripLog: TripLogRepository,
    private val speech: SpeechRepository,
    private val voice: VoiceRepository,
) : ViewModel() {
    private val _uiState: MutableStateFlow<TripLogUiState>
    val uiState: StateFlow<TripLogUiState>

    private val savedChannel = Channel<Unit>(Channel.BUFFERED)
    val saved: Flow<Unit> = savedChannel.receiveAsFlow()
    private var listenJob: Job? = null
    private var spoken = false

    init {
        val v = vehicle.status.value
        val trip = driving.businessTrip
        val start = trip?.startOdometerKm ?: v.odometerKm
        // 가짜 차량은 계기판이 늘지 않으니 시연용 거리(43km)를 쓴다.
        val end = if (v.odometerKm > start) v.odometerKm else start + FAKE_DISTANCE_KM
        val distance = end - start
        val price = tripLog.fuelPricePerLiter()
        val cost = ((distance / v.avgFuelEconomy * price) / 100).roundToInt() * 100
        val p = profile.profile()
        _uiState = MutableStateFlow(
            TripLogUiState(
                entry = TripLogEntry(
                    date = LocalDate.now(),
                    user = "${p.department} · ${p.name}",
                    startOdometerKm = start,
                    endOdometerKm = end,
                    purpose = trip?.event?.title ?: "업무",
                    fuelCostWon = cost,
                ),
                fuelFormula = "${distance}km ÷ 평균 연비 ${v.avgFuelEconomy}km/L × [지역 평균 유가] %,d원/L".format(price),
            )
        )
        uiState = _uiState.asStateFlow()
    }

    fun onEnter() {
        if (spoken) return
        spoken = true
        val purpose = _uiState.value.entry.purpose
        viewModelScope.launch { voice.speak("운행기록을 채웠어요. 목적은 ${withIGa(purpose)} 맞나요?") }
    }

    /** "맞아요, 저장" */
    fun onSave() {
        stopAll()
        viewModelScope.launch {
            tripLog.save(_uiState.value.entry)
            _uiState.update { it.copy(purposeConfirmed = true) }
            savedChannel.send(Unit)
        }
    }

    /** "목적 바꾸기": 말로 받는다. */
    fun onChangePurpose() {
        stopAll()
        _uiState.update { it.copy(editing = VoiceInput(listening = true)) }
        listenJob = viewModelScope.launch {
            speech.listen().collect { e ->
                when (e) {
                    SpeechEvent.Listening -> updateEdit { it.copy(listening = true) }
                    is SpeechEvent.Partial -> updateEdit { it.copy(transcript = e.text) }
                    is SpeechEvent.Failed -> updateEdit { VoiceInput(error = e.reason) }
                    is SpeechEvent.Final -> _uiState.update {
                        it.copy(entry = it.entry.copy(purpose = e.text.trim()), editing = null)
                    }
                }
            }
        }
    }

    private fun updateEdit(f: (VoiceInput) -> VoiceInput) = _uiState.update { s -> s.copy(editing = s.editing?.let(f)) }

    fun stopAll() {
        listenJob?.cancel()
        voice.stop()
    }

    override fun onCleared() = stopAll()

    companion object {
        private const val FAKE_DISTANCE_KM = 43

        fun factory(c: AppContainer) = viewModelFactory {
            initializer {
                TripLogViewModel(
                    c.drivingAssistant, c.vehicleRepository, c.profileRepository,
                    c.tripLogRepository, c.speechRepository, c.voiceRepository,
                )
            }
        }
    }
}

@Composable
fun TripLogScreen(viewModel: TripLogViewModel, onSaved: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.saved.collect { onSaved() } }
    DisposableEffect(viewModel) {
        viewModel.onEnter()
        onDispose { viewModel.stopAll() }
    }
    TripLogContent(state, viewModel::onSave, viewModel::onChangePurpose)
}

@Composable
fun TripLogContent(state: TripLogUiState, onSave: () -> Unit, onChange: () -> Unit) {
    val c = appColors
    val e = state.entry
    val dow = e.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
    AppScreen(AppIcon.Parking, "주차 중 · 출장지 도착") {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(72.dp)) {
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterVertically),
            ) {
                Text(
                    buildAnnotatedString {
                        append("운행기록을 채웠어요.\n목적은 ")
                        withStyle(SpanStyle(color = c.accent)) { append(e.purpose) }
                        append(withIGa(e.purpose).removePrefix(e.purpose))
                        append("\n맞나요?")
                    },
                    style = ds(84, 800, -0.03, 1.24),
                )
                Text("오늘 일정에 있는 미팅으로 짐작했어요. 다르면 말로 알려주세요.", style = ds(34, lineHeight = 1.5, color = c.textSub))
                state.editing?.let { voice ->
                    VoicePromptBox(
                        voice = voice, example = "거래처 납품", confirmedText = "이 목적으로 바꿀게요",
                        onRetry = onChange, title = "목적을 말씀해 주세요", background = c.card,
                    )
                }
                Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    AppButton("맞아요, 저장", onSave, Modifier.weight(1f))
                    AppButton("목적 바꾸기", onChange, Modifier.weight(1f), kind = ButtonKind.Secondary, fontSize = 40)
                }
            }
            Column(
                Modifier
                    .width(760.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(44.dp))
                    .background(c.card)
                    .padding(horizontal = 48.dp, vertical = 40.dp),
            ) {
                Text("업무용승용차 운행기록", style = ds(34, 700, -0.01), modifier = Modifier.padding(bottom = 4.dp))
                Text("국가법령정보센터 별지 서식 항목 기준", style = ds(24, color = c.textSub), modifier = Modifier.padding(bottom = 12.dp))
                LogRow("사용일자", "${e.date.monthValue}월 ${e.date.dayOfMonth}일 ($dow)")
                LogRow("사용자", e.user)
                LogRow("주행 전 계기판", "%,dkm".format(e.startOdometerKm))
                LogRow("주행 후 계기판", "%,dkm".format(e.endOdometerKm))
                LogRow("주행거리 · 일반 업무용", "${e.distanceKm}km")
                LogRow("비고 (운행 목적)", if (state.purposeConfirmed) e.purpose else "${e.purpose} · 확인 대기", valueColor = c.accent)
                Spacer(Modifier.weight(1f))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(c.subtle)
                        .padding(horizontal = 32.dp, vertical = 26.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(Modifier.fillMaxWidth()) {
                        Text("이번 주행 유류비", style = ds(30, 700), modifier = Modifier.weight(1f).alignByBaseline())
                        Text("약 %,d원".format(e.fuelCostWon), style = ds(44, 800, -0.02, tabular = true), modifier = Modifier.alignByBaseline())
                    }
                    Text(state.fuelFormula, style = ds(24, lineHeight = 1.5, color = c.textSub, tabular = true))
                }
            }
        }
    }
}

@Composable
private fun LogRow(label: String, value: String, valueColor: Color = appColors.text) {
    val c = appColors
    Column {
        HorizontalDivider(thickness = 1.dp, color = c.divider)
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
            Text(label, style = ds(28, color = c.textSub), modifier = Modifier.weight(1f).alignByBaseline())
            Text(value, style = ds(30, 700, color = valueColor, tabular = true), modifier = Modifier.alignByBaseline())
        }
    }
}
