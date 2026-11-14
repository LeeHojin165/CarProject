package com.example.carproject.ui.arrival

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.carproject.ui.components.AppButton
import com.example.carproject.ui.components.AppIcon
import com.example.carproject.ui.components.AppScreen
import com.example.carproject.ui.components.ButtonKind
import com.example.carproject.ui.components.CardTitle
import com.example.carproject.ui.components.CheckRow
import com.example.carproject.ui.theme.appColors
import com.example.carproject.ui.theme.ds

@Composable
fun ArrivalScreen(viewModel: ArrivalViewModel, onDone: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DisposableEffect(viewModel) {
        viewModel.onEnter()
        onDispose { viewModel.stop() }
    }
    ArrivalContent(state, onFix = viewModel::onFix, onDone = { viewModel.stop(); onDone() })
}

@Composable
fun ArrivalContent(state: ArrivalUiState, onFix: () -> Unit, onDone: () -> Unit) {
    val c = appColors
    AppScreen(AppIcon.Parking, state.statusText) {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(80.dp)) {
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterVertically),
            ) {
                Text(state.title, style = ds(92, 800, -0.03, 1.22))
                Text(state.subtitle, style = ds(34, lineHeight = 1.5, color = c.textSub))
                Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    if (state.issue != null || state.closing) {
                        AppButton(if (state.closing) "닫는 중…" else "창문 닫기", onFix, Modifier.weight(1f))
                        AppButton("그대로 둘게요", onDone, Modifier.weight(1f), kind = ButtonKind.Secondary, fontSize = 40)
                    } else {
                        AppButton("좋아요", onDone, Modifier.weight(1f))
                    }
                }
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
                CardTitle("내리기 전에 살펴봤어요")
                state.checks.forEachIndexed { i, item ->
                    CheckRow(item.icon, item.label, item.value, showDivider = i > 0, warning = item.warning, verticalPadding = 26.dp)
                }
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(c.subtle)
                        .padding(horizontal = 32.dp, vertical = 28.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(state.tripLabel, style = ds(30, color = c.textSub), modifier = Modifier.alignByBaseline())
                    Text(state.tripSummary, style = ds(36, 700, tabular = true), modifier = Modifier.alignByBaseline())
                }
            }
        }
    }
}
