package com.example.carproject.ui.pickup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.carproject.AppContainer
import com.example.carproject.data.model.Reservation
import com.example.carproject.data.repository.VehicleRepository
import com.example.carproject.data.repository.VoiceRepository
import com.example.carproject.data.repository.SettingsRepository
import androidx.compose.runtime.rememberCoroutineScope
import com.example.carproject.driving.DrivingAssistant
import com.example.carproject.ui.components.AppButton
import com.example.carproject.ui.components.AppIcon
import com.example.carproject.ui.components.AppScreen
import com.example.carproject.ui.components.ButtonKind
import com.example.carproject.ui.components.CardTitle
import com.example.carproject.ui.components.CheckRow
import com.example.carproject.ui.components.LineIcon
import com.example.carproject.ui.home.CheckItem
import com.example.carproject.ui.theme.appColors
import com.example.carproject.ui.theme.ds
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val CABIN_TEMP_C = 22
private const val SEAT_HEAT_LEVEL = 2
private const val VENT_SECONDS = 60

// ───────────────────────────── 16 히터 켜 둘까요 ─────────────────────────────

/** 16에서 답한 뒤 상태. Asking → 답함(켬/안 켬) → "다녀왔어요"로 17. */
enum class WaitStage { Asking, KeptOn, LeftOff }

data class PickupWaitUiState(
    val reservation: Reservation?,
    val outsideTempC: Int,
    val batteryPercent: Int,
    val stage: WaitStage = WaitStage.Asking,
)

/** design/16_PickupWait.html: 픽업하러 내린 동안 히터를 켜 둘지 묻는다. */
class PickupWaitViewModel(
    private val vehicle: VehicleRepository,
    private val voice: VoiceRepository,
    private val driving: DrivingAssistant,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        vehicle.status.value.let { v ->
            PickupWaitUiState(driving.reservation, v.outsideTempC, v.evBatteryPercent ?: v.fuelPercent ?: 0)
        }
    )
    val uiState: StateFlow<PickupWaitUiState> = _uiState.asStateFlow()
    private var spoken = false

    fun onEnter() {
        if (spoken) return
        spoken = true
        viewModelScope.launch {
            voice.speak("다녀오는 동안 히터를 켜 둘까요? 바깥은 ${_uiState.value.outsideTempC}도예요.")
        }
    }

    /** "켜 둘게요": 물은 뒤 눌렀을 때만 실행한다. */
    fun onKeepOn() {
        voice.stop()
        viewModelScope.launch {
            vehicle.keepClimate(CABIN_TEMP_C, SEAT_HEAT_LEVEL)
            _uiState.update { it.copy(stage = WaitStage.KeptOn) }
            voice.speak("${CABIN_TEMP_C}도로 맞춰 둘게요. 다녀오세요.")
        }
    }

    /** "다녀왔어요": 환기 묻기가 켜져 있으면 17로, 꺼져 있으면 픽업을 마치고 내비로. true면 17로 간다. */
    suspend fun onReturned(): Boolean {
        voice.stop()
        if (settings.proactive.value.ventilate) return true
        driving.finishPickup()
        return false
    }

    fun onLeaveOff() {
        voice.stop()
        _uiState.update { it.copy(stage = WaitStage.LeftOff) }
    }

    fun stop() = voice.stop()
    override fun onCleared() = stop()

    companion object {
        fun factory(c: AppContainer) = viewModelFactory {
            initializer { PickupWaitViewModel(c.vehicleRepository, c.voiceRepository, c.drivingAssistant, c.settingsRepository) }
        }
    }
}

@Composable
fun PickupWaitScreen(viewModel: PickupWaitViewModel, onReturned: (toDepart: Boolean) -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    DisposableEffect(viewModel) {
        viewModel.onEnter()
        onDispose { viewModel.stop() }
    }
    PickupWaitContent(state, viewModel::onKeepOn, viewModel::onLeaveOff) {
        scope.launch { onReturned(viewModel.onReturned()) }
    }
}

@Composable
fun PickupWaitContent(state: PickupWaitUiState, onKeepOn: () -> Unit, onLeaveOff: () -> Unit, onBack: () -> Unit) {
    val c = appColors
    val strong = SpanStyle(color = c.text, fontWeight = FontWeight(700))
    QuestionLayout(
        status = "주차 중 · 픽업 가게 앞",
        title = when (state.stage) {
            WaitStage.Asking -> AnnotatedString("다녀오는 동안\n히터를 켜 둘까요?")
            WaitStage.KeptOn -> AnnotatedString("히터를 켜 뒀어요.\n다녀오세요.")
            WaitStage.LeftOff -> AnnotatedString("히터는 끌게요.\n다녀오세요.")
        },
        subtitle = buildAnnotatedString {
            if (state.stage == WaitStage.Asking) {
                append("바깥은 "); withStyle(strong) { append("${state.outsideTempC}도") }
                append("예요. 돌아왔을 때 차 안을 "); withStyle(strong) { append("${CABIN_TEMP_C}도") }
                append("로 맞춰 둘게요.")
            } else {
                append("픽업하고 돌아오면 아래 버튼을 눌러 주세요. 집까지 안내를 이어갈게요.")
            }
        },
        buttons = {
            if (state.stage == WaitStage.Asking) {
                AppButton("켜 둘게요", onKeepOn, Modifier.weight(1f))
                AppButton("괜찮아요", onLeaveOff, Modifier.weight(1f), kind = ButtonKind.Secondary, fontSize = 40)
            } else {
                // 디자인에 없는 연결 장면: 다녀온 뒤 17로 넘어간다.
                AppButton("다녀왔어요", onBack, Modifier.weight(1f))
            }
        },
        cardTitle = "켜 두면 이렇게 할게요",
        rows = listOf(
            CheckItem(AppIcon.Thermometer, "실내 온도", "${CABIN_TEMP_C}도 유지"),
            CheckItem(AppIcon.SeatHeat, "운전석 열선", "${SEAT_HEAT_LEVEL}단 유지"),
            CheckItem(AppIcon.Battery, "배터리", "${state.batteryPercent}% → 약 2% 사용"),
            CheckItem(AppIcon.Lock, "문", "내리면 잠가 둘게요"),
        ),
    ) {
        state.reservation?.let { r ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(c.subtle)
                    .padding(horizontal = 32.dp, vertical = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("픽업 번호", style = ds(26, color = c.textSub))
                    Text(r.pickupNumber, style = ds(52, 800, 0.04, tabular = true))
                }
                Text(
                    "폰으로도 보냈어요\n%d:%02d까지 픽업".format(r.payUntil.hour, r.payUntil.minute),
                    style = ds(28, lineHeight = 1.5, color = c.textSub),
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

// ───────────────────────────── 17 환기할까요 ─────────────────────────────

data class PickupDepartUiState(val savedKg: Double, val working: Boolean = false)

/** design/17_PickupDepart.html: 음식 냄새가 배지 않게 환기할지 묻는다. */
class PickupDepartViewModel(
    private val vehicle: VehicleRepository,
    private val voice: VoiceRepository,
    private val driving: DrivingAssistant,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PickupDepartUiState(driving.reservation?.store?.weightKg ?: 0.0))
    val uiState: StateFlow<PickupDepartUiState> = _uiState.asStateFlow()

    private val doneChannel = Channel<Unit>(Channel.BUFFERED)
    val done: Flow<Unit> = doneChannel.receiveAsFlow()
    private var spoken = false

    fun onEnter() {
        if (spoken) return
        spoken = true
        viewModelScope.launch { voice.speak("음식 냄새가 배지 않게 1분만 환기할까요?") }
    }

    /** "환기할게요": 물은 뒤 눌렀을 때만 실행한다. */
    fun onVentilate() = finish(ventilate = true)

    fun onSkip() = finish(ventilate = false)

    private fun finish(ventilate: Boolean) {
        if (_uiState.value.working) return
        voice.stop()
        _uiState.update { it.copy(working = true) }
        viewModelScope.launch {
            if (ventilate) vehicle.ventilate(VENT_SECONDS)
            driving.finishPickup()
            doneChannel.send(Unit)
        }
    }

    fun stop() = voice.stop()
    override fun onCleared() = stop()

    companion object {
        fun factory(c: AppContainer) = viewModelFactory {
            initializer { PickupDepartViewModel(c.vehicleRepository, c.voiceRepository, c.drivingAssistant) }
        }
    }
}

@Composable
fun PickupDepartScreen(viewModel: PickupDepartViewModel, onDone: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.done.collect { onDone() } }
    DisposableEffect(viewModel) {
        viewModel.onEnter()
        onDispose { viewModel.stop() }
    }
    PickupDepartContent(state, viewModel::onVentilate, viewModel::onSkip)
}

@Composable
fun PickupDepartContent(state: PickupDepartUiState, onVentilate: () -> Unit, onSkip: () -> Unit) {
    val c = appColors
    QuestionLayout(
        status = "주차 중 · 픽업 완료",
        title = AnnotatedString("음식 냄새가 배지 않게\n1분만 환기할까요?"),
        subtitle = AnnotatedString("창문을 조금만 열었다가, 1분 뒤에 다시 닫을게요."),
        buttons = {
            AppButton(if (state.working) "환기하는 중…" else "환기할게요", onVentilate, Modifier.weight(1f))
            AppButton("괜찮아요", onSkip, Modifier.weight(1f), kind = ButtonKind.Secondary, fontSize = 40)
        },
        cardTitle = "환기하면 이렇게 할게요",
        rows = listOf(
            CheckItem(AppIcon.Window, "창문", "앞좌석 조금 열기"),
            CheckItem(AppIcon.Timer, "시간", "1분 뒤 자동으로 닫힘"),
            CheckItem(AppIcon.Thermometer, "히터", "계속 ${CABIN_TEMP_C}도"),
        ),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(c.subtle)
                .padding(horizontal = 32.dp, vertical = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LineIcon(AppIcon.Leaf, 48.dp, color = c.link, strokeWidth = 2f)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("오늘 음식 ${state.savedKg}kg을 구했어요", style = ds(32, 700, tabular = true))
                Text("버려질 뻔한 음식을 기록해 뒀어요", style = ds(26, color = c.textSub))
            }
        }
    }
}

// ───────────────────────────── 공통 틀 ─────────────────────────────

/** 16·17(그리고 09·10과 같은) 질문 화면 틀: 왼쪽 질문·버튼, 오른쪽 660 카드(목록 + 아래 상자). */
@Composable
private fun QuestionLayout(
    status: String,
    title: AnnotatedString,
    subtitle: AnnotatedString,
    buttons: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
    cardTitle: String,
    rows: List<CheckItem>,
    bottom: @Composable ColumnScope.() -> Unit,
) {
    val c = appColors
    AppScreen(AppIcon.Parking, status) {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(80.dp)) {
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterVertically),
            ) {
                Text(title, style = ds(92, 800, -0.03, 1.22))
                Text(subtitle, style = ds(34, lineHeight = 1.5, color = c.textSub))
                Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp), content = buttons)
            }
            Column(
                Modifier
                    .width(660.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(44.dp))
                    .background(c.card)
                    .padding(horizontal = 48.dp, vertical = 44.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CardTitle(cardTitle)
                rows.forEachIndexed { i, item ->
                    CheckRow(item.icon, item.label, item.value, showDivider = i > 0, verticalPadding = 26.dp)
                }
                Spacer(Modifier.weight(1f))
                bottom()
            }
        }
    }
}
