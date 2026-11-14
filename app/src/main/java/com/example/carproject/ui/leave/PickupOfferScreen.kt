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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.carproject.AppContainer
import com.example.carproject.data.model.PickupStore
import com.example.carproject.data.repository.PickupRepository
import com.example.carproject.data.repository.RouteResult
import com.example.carproject.data.repository.VoiceRepository
import com.example.carproject.driving.DrivingAssistant
import com.example.carproject.ui.components.AppButton
import com.example.carproject.ui.components.ButtonKind
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

data class PickupOfferUiState(
    val stores: List<PickupStore>,
    val selected: Int = 0,
    val departing: Boolean = false,
    val error: String? = null,
)

/** design/13_PickupOffer.html: 퇴근길 위 마감 할인 가게. */
class PickupOfferViewModel(
    pickup: PickupRepository,
    private val voice: VoiceRepository,
    private val driving: DrivingAssistant,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PickupOfferUiState(pickup.storesOnRouteHome()))
    val uiState: StateFlow<PickupOfferUiState> = _uiState.asStateFlow()

    private val departedChannel = Channel<RouteResult>(Channel.BUFFERED)
    val departed: Flow<RouteResult> = departedChannel.receiveAsFlow()
    private var spoken = false

    fun onEnter() {
        if (spoken) return
        spoken = true
        val n = _uiState.value.stores.size
        viewModelScope.launch { voice.speak("퇴근길에 반값 픽업, ${n}곳 있어요.") }
    }

    fun onSelect(index: Int) = _uiState.update { it.copy(selected = index) }

    /** "오늘은 그냥 갈게요": 고른 충전소만 들러 집으로 출발한다. */
    fun onSkip() {
        voice.stop()
        if (_uiState.value.departing) return
        _uiState.update { it.copy(departing = true, error = null) }
        viewModelScope.launch {
            val r = driving.departHome()
            _uiState.update { it.copy(departing = false, error = (r as? RouteResult.Failed)?.reason) }
            if (r !is RouteResult.Failed) departedChannel.send(r)
        }
    }

    fun stop() = voice.stop()
    override fun onCleared() = stop()

    companion object {
        fun factory(c: AppContainer) = viewModelFactory {
            initializer { PickupOfferViewModel(c.pickupRepository, c.voiceRepository, c.drivingAssistant) }
        }
    }
}

private val MarkerPositions = listOf(1036f to 752f, 1290f to 470f, 1560f to 222f)

/** "2,900원" */
fun won(price: Int) = "%,d원".format(price)

@Composable
fun PickupOfferScreen(viewModel: PickupOfferViewModel, onDetail: (PickupStore) -> Unit, onDeparted: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.departed.collect { onDeparted() } }
    DisposableEffect(viewModel) {
        viewModel.onEnter()
        onDispose { viewModel.stop() }
    }
    PickupOfferContent(state, viewModel::onSelect, { onDetail(state.stores[state.selected]) }, viewModel::onSkip)
}

@Composable
fun PickupOfferContent(state: PickupOfferUiState, onSelect: (Int) -> Unit, onDetail: () -> Unit, onSkip: () -> Unit) {
    val c = appColors
    Box(Modifier.fillMaxSize()) {
        FakeRouteMap {
            state.stores.forEachIndexed { i, st ->
                val (x, y) = MarkerPositions[i]
                At(x, y) { NumberedMapPill(i + 1, "${st.discountPercent}% · ${won(st.price)}", selected = i == state.selected) }
            }
        }
        MapSidePanel(720.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
                PanelHeader("주차 중 · 퇴근 준비")
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("퇴근길에 반값 픽업,\n${state.stores.size}곳 있어요.", style = ds(60, 800, -0.03, 1.24))
                    Text("집 가는 길에서 5분 안쪽으로 돌아가는 곳만 골랐어요.", style = ds(28, lineHeight = 1.5, color = c.textSub))
                }
                Column {
                    state.stores.forEachIndexed { i, st ->
                        NumberedListItem(
                            number = i + 1,
                            title = st.name,
                            trailing = "${st.remaining}개 남음",
                            trailingColor = if (st.remaining <= 1) c.warning else c.link,
                            sub = "${st.category} · ${hm(st.pickupStart)}~${hm(st.pickupEnd)} 픽업",
                            subTrailing = "+${st.detourMinutes}분",
                            selected = i == state.selected,
                            showDivider = i > 0 && i != state.selected && i - 1 != state.selected,
                            verticalPadding = 18.dp,
                            onClick = { onSelect(i) },
                        )
                    }
                }
                state.error?.let { Text(it, style = ds(28, 600, color = c.warning)) }
            }
            Spacer(Modifier.weight(1f))
            AppButton(
                "${state.selected + 1}번 가게 자세히 보기", onDetail, Modifier.fillMaxWidth(),
                height = 120.dp, radius = 34.dp, fontSize = 42, tracking = 0.0,
            )
            Spacer(Modifier.height(16.dp))
            AppButton(
                if (state.departing) "경로 찾는 중…" else "오늘은 그냥 갈게요", onSkip, Modifier.fillMaxWidth(),
                kind = ButtonKind.Secondary, height = 104.dp, radius = 32.dp, fontSize = 36,
            )
        }
    }
}

fun hm(t: java.time.LocalTime) = "%02d:%02d".format(t.hour, t.minute)
