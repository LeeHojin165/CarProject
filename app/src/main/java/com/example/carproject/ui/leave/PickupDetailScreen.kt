package com.example.carproject.ui.leave

import com.example.carproject.data.AppClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.carproject.AppContainer
import com.example.carproject.data.model.PickupStore
import com.example.carproject.data.repository.PickupRepository
import com.example.carproject.data.repository.RouteResult
import com.example.carproject.driving.DrivingAssistant
import com.example.carproject.ui.components.AppButton
import com.example.carproject.ui.components.AppIcon
import com.example.carproject.ui.components.ButtonKind
import com.example.carproject.ui.components.LineIcon
import com.example.carproject.ui.components.withIGa
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
import java.time.LocalTime

data class PickupDetailUiState(
    val store: PickupStore,
    val confirmTimeText: String,
    val arrivalText: String,
    val arrivalOk: Boolean,
    val reserving: Boolean = false,
    val error: String? = null,
)

/** design/14_PickupDetail.html */
class PickupDetailViewModel(
    savedState: SavedStateHandle,
    private val pickup: PickupRepository,
    private val driving: DrivingAssistant,
) : ViewModel() {
    private val store = pickup.store(savedState.get<String>(ARG_STORE_ID).orEmpty()) ?: pickup.storesOnRouteHome().first()

    private val _uiState = MutableStateFlow(build())
    val uiState: StateFlow<PickupDetailUiState> = _uiState.asStateFlow()

    private val departedChannel = Channel<RouteResult>(Channel.BUFFERED)
    val departed: Flow<RouteResult> = departedChannel.receiveAsFlow()

    private fun build(): PickupDetailUiState {
        // 회사에서 가게까지 가짜 소요시간: 경로 위라 12분 + 돌아가는 시간.
        val arrival = AppClock.now().plusMinutes(12L + store.detourMinutes)
        val ok = !arrival.isAfter(store.pickupEnd)
        val note = when {
            !ok -> "픽업 시간이 지나요"
            arrival.isBefore(store.pickupStart) -> "${hm(store.pickupStart)}부터 받을 수 있어요"
            else -> "시간 안에 도착해요"
        }
        return PickupDetailUiState(
            store = store,
            confirmTimeText = hm(store.pickupStart.minusMinutes(30)),
            arrivalText = "${hm(arrival)} · $note",
            arrivalOk = ok,
        )
    }

    /** "예약하고 경유지 추가": 화면에서 가격·시간을 보여준 뒤 눌렀을 때만 결제·예약한다. */
    fun onReserve() {
        if (_uiState.value.reserving) return
        _uiState.update { it.copy(reserving = true, error = null) }
        viewModelScope.launch {
            driving.setReservation(pickup.reserve(store))
            val r = driving.departHome()
            _uiState.update { it.copy(reserving = false, error = (r as? RouteResult.Failed)?.reason) }
            if (r !is RouteResult.Failed) departedChannel.send(r)
        }
    }

    companion object {
        const val ARG_STORE_ID = "storeId"

        fun factory(c: AppContainer) = viewModelFactory {
            initializer { PickupDetailViewModel(createSavedStateHandle(), c.pickupRepository, c.drivingAssistant) }
        }
    }
}

@Composable
fun PickupDetailScreen(viewModel: PickupDetailViewModel, onBack: () -> Unit, onDeparted: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.departed.collect { onDeparted() } }
    PickupDetailContent(state, onBack, viewModel::onReserve)
}

@Composable
fun PickupDetailContent(state: PickupDetailUiState, onBack: () -> Unit, onReserve: () -> Unit) {
    val c = appColors
    val st = state.store
    Box(Modifier.fillMaxSize()) {
        FakeRouteMap(showWorkLabel = false) {
            At(1124f, 776f) {
                Text(
                    "경유지 · +${st.detourMinutes}분",
                    style = ds(26, 700, color = c.onAccent),
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(c.accent)
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
        }
        MapSidePanel(860.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    // 디자인은 76이지만 버튼 최소 높이 규칙(96)을 따른다.
                    Box(
                        Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(c.subtle)
                            .clickable(role = Role.Button, onClick = onBack),
                        contentAlignment = Alignment.Center,
                    ) { LineIcon(AppIcon.Back, 36.dp, color = c.text, strokeWidth = 2.4f) }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(st.name, style = ds(44, 800, -0.02, 1.2))
                        Text(st.detailCategory, style = ds(26, color = c.textSub))
                    }
                    Text(
                        "${st.remaining}개 예약 가능",
                        style = ds(26, 700, color = c.text),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(androidx.compose.ui.graphics.Color(0xFF002C5F))
                            .padding(horizontal = 22.dp, vertical = 12.dp),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.padding(top = 4.dp)) {
                    Text("${st.discountPercent}%", style = ds(44, 800, color = c.warning), modifier = Modifier.alignByBaseline())
                    Text(won(st.price), style = ds(60, 800, -0.03, tabular = true), modifier = Modifier.alignByBaseline())
                    Text(
                        won(st.originalPrice),
                        style = ds(30, color = c.textSub, tabular = true).copy(textDecoration = TextDecoration.LineThrough),
                        modifier = Modifier.alignByBaseline(),
                    )
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(c.card)
                        .padding(horizontal = 28.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(Modifier.fillMaxWidth()) {
                        Text("픽업 시간 · ${state.confirmTimeText} 확정", style = ds(28, color = c.textSub, tabular = true), modifier = Modifier.weight(1f).alignByBaseline())
                        Text("오늘 ${hm(st.pickupStart)} ~ ${hm(st.pickupEnd)}", style = ds(32, 700, tabular = true), modifier = Modifier.alignByBaseline())
                    }
                    Row(Modifier.fillMaxWidth()) {
                        Text("내 도착 예상", style = ds(28, color = c.textSub), modifier = Modifier.weight(1f).alignByBaseline())
                        Text(
                            state.arrivalText,
                            style = ds(32, 700, color = if (state.arrivalOk) c.link else c.warning, tabular = true),
                            modifier = Modifier.alignByBaseline(),
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("이런 구성이에요", style = ds(30, 700))
                    Text("그날 남은 ${withIGa(st.itemNoun)} 무작위로 담겨요. 자주 담기는 메뉴는 이래요.", style = ds(28, lineHeight = 1.5, color = c.textSub))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        st.tags.forEach { tag ->
                            Text(
                                tag, style = ds(26, 600),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(c.subtle)
                                    .padding(horizontal = 22.dp, vertical = 10.dp),
                            )
                        }
                    }
                    Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(2) {
                            ReviewTile(Modifier.weight(1f)) {
                                LineIcon(AppIcon.Image, 40.dp, color = c.textSub, strokeWidth = 2f)
                                Text("[리뷰 사진]", style = ds(22, color = c.textSub))
                            }
                        }
                        ReviewTile(Modifier.weight(1f)) {
                            Text("리뷰 전체 보기", style = ds(26, 700))
                            Text("사진 리뷰 [n]개", style = ds(22, color = c.textSub))
                        }
                    }
                }
                Text(
                    buildAnnotatedString {
                        val strong = SpanStyle(color = c.text, fontWeight = FontWeight(700))
                        append("보관 "); withStyle(strong) { append(st.storage) }
                        append("      섭취 "); withStyle(strong) { append(st.eatBy) }
                        append("      픽업 후 집까지 "); withStyle(strong) { append("${st.minutesToHomeAfter}분") }
                    },
                    style = ds(26, color = c.textSub),
                )
                state.error?.let { Text(it, style = ds(28, 600, color = c.warning)) }
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AppButton(
                    if (state.reserving) "예약하는 중…" else "예약하고 경유지 추가", onReserve, Modifier.weight(1.6f),
                    height = 120.dp, radius = 34.dp, fontSize = 40, tracking = 0.0,
                )
                AppButton(
                    "다른 가게 보기", onBack, Modifier.weight(1f),
                    kind = ButtonKind.Secondary, height = 120.dp, radius = 34.dp, fontSize = 34,
                )
            }
        }
    }
}

@Composable
private fun ReviewTile(modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier.height(140.dp).clip(RoundedCornerShape(22.dp)).background(appColors.subtle),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}
