package com.example.carproject.ui.parking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.carproject.data.model.ParkingSpot
import com.example.carproject.ui.components.AppButton
import com.example.carproject.ui.components.AppIcon
import com.example.carproject.ui.components.AppScreen
import com.example.carproject.ui.components.ButtonKind
import com.example.carproject.ui.components.VoicePromptBox
import com.example.carproject.ui.theme.appColors
import com.example.carproject.ui.theme.ds

private val Floors = listOf("B1", "B2", "B3", "B4", "지상")

/** design/07_ParkAsk.html */
@Composable
fun ParkAskScreen(viewModel: ParkAskViewModel, onSaved: (ParkingSpot) -> Unit, onSkip: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.saved.collect(onSaved) }
    // 화면에 들어올 때마다(08에서 "다시 말하기"로 돌아올 때 포함) 다시 듣고, 나가면 멈춘다.
    DisposableEffect(viewModel) {
        viewModel.onEnter()
        onDispose { viewModel.stop() }
    }
    ParkAskContent(
        state,
        onRetryVoice = viewModel::onRetryVoice,
        onFloorClick = viewModel::onFloorClick,
        onSameAsLast = viewModel::onSameAsLast,
        onSkip = { viewModel.stop(); onSkip() },
    )
}

@Composable
fun ParkAskContent(
    state: ParkAskUiState,
    onRetryVoice: () -> Unit,
    onFloorClick: (String) -> Unit,
    onSameAsLast: () -> Unit,
    onSkip: () -> Unit,
) {
    val c = appColors
    AppScreen(AppIcon.Parking, "주차 중 · 방금 P로 바꿨어요") {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(72.dp)) {
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(36.dp, Alignment.CenterVertically),
            ) {
                Text("주차했어요.\n어디에 세웠나요?", style = ds(84, 800, -0.03, 1.22))
                Text(
                    "지하에서는 위치가 정확하지 않아서, 층과 기둥 번호를 말로 남겨 두면 좋아요.",
                    style = ds(34, lineHeight = 1.5, color = c.textSub),
                )
                VoicePromptBox(
                    voice = state.voice,
                    example = "지하 3층 기둥 C12",
                    confirmedText = "이렇게 기억할게요",
                    onRetry = onRetryVoice,
                    barHeights = listOf(22, 44, 64, 36, 54, 26, 46),
                    titleSize = 40,
                    subSize = 28,
                    radius = 36.dp,
                    background = c.card,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Floors.forEach { floor ->
                        val selected = state.selectedFloor == floor
                        Box(
                            Modifier
                                .weight(1f)
                                .height(104.dp)
                                .clip(RoundedCornerShape(30.dp))
                                .background(if (selected) c.accent else c.card)
                                .clickable(role = Role.Button) { onFloorClick(floor) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(floor, style = ds(40, 700, color = if (selected) c.onAccent else c.text, tabular = true))
                        }
                    }
                }
            }
            Column(
                Modifier
                    .width(680.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(44.dp))
                    .background(c.card)
                    .padding(horizontal = 48.dp, vertical = 44.dp),
            ) {
                Text("이미 알고 있는 것", style = ds(34, 700), modifier = Modifier.padding(bottom = 8.dp))
                KnownRow("장소", state.placeLabel, showDivider = false)
                KnownRow("주차한 시각", state.parkedTimeText, showDivider = true)
                state.last?.let { KnownRow("지난번 여기서", it.label, showDivider = true) }
                Spacer(Modifier.weight(1f))
                if (state.last != null) {
                    AppButton(
                        "지난번과 같아요", onSameAsLast, Modifier.fillMaxWidth(),
                        kind = ButtonKind.Secondary, height = 120.dp, radius = 32.dp, fontSize = 36, weight = 700,
                    )
                }
                Box(
                    Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                        .height(96.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .clickable(role = Role.Button, onClick = onSkip),
                    contentAlignment = Alignment.Center,
                ) { Text("이번엔 건너뛰기", style = ds(30, 600, color = c.textSub)) }
            }
        }
    }
}

@Composable
private fun KnownRow(label: String, value: String, showDivider: Boolean) {
    val c = appColors
    Column {
        if (showDivider) HorizontalDivider(thickness = 1.dp, color = c.divider)
        Row(
            Modifier.fillMaxWidth().padding(vertical = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = ds(28, color = c.textSub), maxLines = 1)
            Text(value, style = ds(32, 700, tabular = true), textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        }
    }
}
