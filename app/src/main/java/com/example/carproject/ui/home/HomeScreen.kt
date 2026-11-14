package com.example.carproject.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.carproject.data.model.Place
import com.example.carproject.ui.components.AppButton
import com.example.carproject.ui.components.AppIcon
import com.example.carproject.ui.components.AppScreen
import com.example.carproject.ui.components.ButtonKind
import com.example.carproject.ui.components.CardTitle
import com.example.carproject.ui.components.CheckRow
import com.example.carproject.ui.theme.CarProjectTheme
import com.example.carproject.ui.theme.DesignCanvas
import com.example.carproject.ui.theme.appColors
import com.example.carproject.ui.theme.ds

/**
 * design/01_Main.html: 출발 전 첫 화면, 어디 가는지 먼저 묻는다.
 * 02_DestConfirm·03_DestAsk는 이 화면 위에 뜨는 대화상자다.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onDestinationChosen: (Place) -> Unit,
    onLeaveWork: () -> Unit,
    onBusinessTrip: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.destinationChosen.collect(onDestinationChosen) }
    val choose: (Place) -> Unit = viewModel::chooseDestination
    Box(Modifier.fillMaxSize()) {
        HomeContent(state, viewModel::onCommuteClick, { viewModel.onLeaveWorkClick(); onLeaveWork() }, onBusinessTrip)
        when (val d = state.dialog) {
            HomeDialog.None -> Unit
            is HomeDialog.ConfirmWork -> ConfirmWorkDialog(
                d, onGoToWork = choose, onOtherPlace = viewModel::onOtherPlaceClick, onDismiss = viewModel::dismissDialog,
            )
            is HomeDialog.AskDestination -> AskDestinationDialog(
                d, onPlaceChosen = choose, onBackToWork = viewModel::showConfirmWork,
                onRetryVoice = viewModel::startListening, onDismiss = viewModel::dismissDialog,
            )
        }
    }
}

@Composable
fun HomeContent(
    state: HomeUiState,
    onCommute: () -> Unit,
    onLeaveWork: () -> Unit,
    onBusinessTrip: () -> Unit,
) {
    val c = appColors
    AppScreen(AppIcon.Parking, "주차 중 · 출발 전") {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(80.dp)) {
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterVertically),
            ) {
                Text("좋은 아침이에요.\n출근하시나요?", style = ds(92, 800, -0.03, 1.22))
                Text(state.hint, style = ds(34, lineHeight = 1.5, color = c.textSub))
                Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    AppButton("네, 출근해요", onCommute, Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        AppButton("퇴근이에요", onLeaveWork, Modifier.weight(1f), ButtonKind.Secondary, 120.dp, 32.dp)
                        AppButton("출장 가요", onBusinessTrip, Modifier.weight(1f), ButtonKind.Secondary, 120.dp, 32.dp)
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
                CardTitle("출발 전에 살펴봤어요")
                state.checks.forEachIndexed { i, item ->
                    CheckRow(item.icon, item.label, item.value, showDivider = i > 0, warning = item.warning)
                }
                Spacer(Modifier.weight(1f))
                Text(state.footnote, style = ds(28, lineHeight = 1.5, color = c.textSub))
            }
        }
    }
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun HomePreview() = CarProjectTheme {
    DesignCanvas {
        HomeContent(
            HomeUiState(
                hint = "화요일 이 시간에는 보통 회사로 출발하셨어요.",
                checks = listOf(
                    CheckItem(AppIcon.Battery, "배터리", "62% · 280km"),
                    CheckItem(AppIcon.Tire, "타이어 공기압", "네 바퀴 정상"),
                    CheckItem(AppIcon.Warning, "경고등", "없음"),
                    CheckItem(AppIcon.Drop, "워셔액", "부족 · 보충 필요", true),
                ),
                footnote = "가는 길 주유소에서 넣을 수 있게, 출발하면 다시 알려드릴게요.",
            ),
            {}, {}, {},
        )
    }
}
