package com.example.carproject.ui.leave

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.carproject.AppContainer
import com.example.carproject.data.model.ChargingStation
import com.example.carproject.data.repository.ChargingRepository
import com.example.carproject.data.repository.OffModeRepository
import com.example.carproject.data.repository.VehicleRepository
import com.example.carproject.data.repository.VoiceRepository
import com.example.carproject.data.repository.SettingsRepository
import androidx.compose.runtime.rememberCoroutineScope
import com.example.carproject.driving.DrivingAssistant
import com.example.carproject.ui.components.AppButton
import com.example.carproject.ui.components.ButtonKind
import com.example.carproject.ui.theme.appColors
import com.example.carproject.ui.theme.ds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChargeRouteUiState(
    val batteryPercent: Int,
    val rangeKm: Int,
    val roundTripKm: Int,
    val homeKm: Int,
    val stations: List<ChargingStation>,
    val selected: Int = 0,
)

/** design/12_ChargeRoute.html: 내일 출근이 빠듯하면 퇴근길 위 충전소를 권한다. */
class ChargeRouteViewModel(
    charging: ChargingRepository,
    offMode: OffModeRepository,
    vehicle: VehicleRepository,
    private val voice: VoiceRepository,
    private val driving: DrivingAssistant,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        vehicle.status.value.let { v ->
            ChargeRouteUiState(v.evBatteryPercent ?: v.fuelPercent ?: 0, v.rangeKm, offMode.tomorrowRoundTripKm(), offMode.homeKm(), charging.stationsOnRouteHome())
        }
    )
    val uiState: StateFlow<ChargeRouteUiState> = _uiState.asStateFlow()
    private var spoken = false

    fun onEnter() {
        if (spoken) return
        spoken = true
        viewModelScope.launch { voice.speak("내일 출근하려면 충전이 필요해요. 가는 길에 들를 충전소를 골라 봤어요.") }
    }

    fun onSelect(index: Int) {
        _uiState.value = _uiState.value.copy(selected = index)
    }

    /** "n번 충전소 경유지로 넣기": 실제 경로는 집으로 출발할 때 함께 요청한다. */
    fun onAddWaypoint() {
        voice.stop()
        val s = _uiState.value
        driving.chooseCharger(s.stations[s.selected])
    }

    fun onChargeAtHome() {
        voice.stop()
        driving.chooseCharger(null)
    }

    /** 다음 장면: 픽업 제안이 꺼져 있으면 바로 집으로 출발한다. true면 출발함. */
    suspend fun departIfNoPickup(): Boolean {
        if (settings.proactive.value.pickupOffer) return false
        driving.departHome()
        return true
    }

    fun stop() = voice.stop()
    override fun onCleared() = stop()

    companion object {
        fun factory(c: AppContainer) = viewModelFactory {
            initializer {
                ChargeRouteViewModel(
                    c.chargingRepository, c.offModeRepository, c.vehicleRepository, c.voiceRepository,
                    c.drivingAssistant, c.settingsRepository,
                )
            }
        }
    }
}

private val MarkerPositions = listOf(1036f to 752f, 1290f to 470f, 1560f to 222f)

@Composable
fun ChargeRouteScreen(viewModel: ChargeRouteViewModel, onNext: (departed: Boolean) -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val next: () -> Unit = { scope.launch { onNext(viewModel.departIfNoPickup()) } }
    DisposableEffect(viewModel) {
        viewModel.onEnter()
        onDispose { viewModel.stop() }
    }
    ChargeRouteContent(
        state,
        onSelect = viewModel::onSelect,
        onAdd = { viewModel.onAddWaypoint(); next() },
        onHome = { viewModel.onChargeAtHome(); next() },
    )
}

@Composable
fun ChargeRouteContent(state: ChargeRouteUiState, onSelect: (Int) -> Unit, onAdd: () -> Unit, onHome: () -> Unit) {
    val c = appColors
    Box(Modifier.fillMaxSize()) {
        FakeRouteMap {
            state.stations.forEachIndexed { i, st ->
                val (x, y) = MarkerPositions[i]
                At(x, y) { NumberedMapPill(i + 1, st.mapLabel, selected = i == state.selected) }
            }
        }
        MapSidePanel(720.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PanelHeader("주차 중 · 퇴근 준비")
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("내일 출근하려면\n충전이 필요해요.", style = ds(54, 800, -0.03, 1.22))
                    val left = state.rangeKm - state.homeKm - state.roundTripKm
                    Text(
                        "배터리 ${state.batteryPercent}% · ${state.rangeKm}km 남았어요. 집까지 ${state.homeKm}km, 내일 왕복 ${state.roundTripKm}km를 가면 " +
                            if (left > 0) "${left}km밖에 안 남아요." else "도착하기 어려워요.",
                        style = ds(28, lineHeight = 1.5, color = c.textSub),
                    )
                }
                Column {
                    state.stations.forEachIndexed { i, st ->
                        NumberedListItem(
                            number = i + 1,
                            title = st.name,
                            trailing = "+${st.detourMinutes}분",
                            trailingColor = c.link,
                            sub = st.description,
                            subTrailing = null,
                            selected = i == state.selected,
                            showDivider = i > 0 && i != state.selected && i - 1 != state.selected,
                            verticalPadding = 12.dp,
                            onClick = { onSelect(i) },
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            AppButton(
                "${state.selected + 1}번 충전소 경유지로 넣기", onAdd, Modifier.fillMaxWidth(),
                height = 108.dp, radius = 34.dp, fontSize = 42, tracking = 0.0,
            )
            Spacer(Modifier.height(16.dp))
            AppButton(
                "집에서 충전할게요", onHome, Modifier.fillMaxWidth(),
                kind = ButtonKind.Secondary, height = 96.dp, radius = 32.dp, fontSize = 36,
            )
        }
    }
}
