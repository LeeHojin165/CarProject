package com.example.carproject.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.carproject.ui.components.AppButton
import com.example.carproject.ui.components.AppIcon
import com.example.carproject.ui.components.AppScreen
import com.example.carproject.ui.components.ButtonKind
import com.example.carproject.ui.components.LineIcon
import com.example.carproject.ui.theme.CarProjectTheme
import com.example.carproject.ui.theme.DesignCanvas
import com.example.carproject.ui.theme.appColors
import com.example.carproject.ui.theme.ds

/** design/00_Login.html */
@Composable
fun LoginScreen(viewModel: LoginViewModel, onLater: () -> Unit, onLoggedIn: () -> Unit = onLater) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(state.isLoggedIn) { if (state.isLoggedIn) onLoggedIn() }
    LoginContent(state, onLater)
}

@Composable
fun LoginContent(state: LoginUiState, onLater: () -> Unit) {
    val c = appColors
    AppScreen(AppIcon.Person, "${state.profileName} 프로필 · 주차 중") {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(80.dp)) {
            // 왼쪽: 안내 문구와 단계
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(36.dp, Alignment.CenterVertically),
            ) {
                Text(
                    buildAnnotatedString {
                        append("처음 오셨네요.\n휴대폰으로 ")
                        withStyle(SpanStyle(color = c.accent)) { append("QR을 찍어") }
                        append("\n연결해 주세요.")
                    },
                    style = ds(76, 800, -0.03, 1.22),
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("휴대폰 카메라로 QR 찍기", "휴대폰에서 구글 계정으로 로그인", "Slack 연결하기")
                        .forEachIndexed { i, label -> StepRow(i + 1, label, active = i == state.doneSteps, done = i < state.doneSteps) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LineIcon(AppIcon.Lock, 36.dp, Modifier.padding(top = 4.dp), color = c.textSub, strokeWidth = 2.2f)
                    Text(
                        "한 번만 하면 돼요. 연결 정보는 이 차의 ${state.profileName} 프로필에만 저장되고, 다른 프로필에서는 보이지 않아요.",
                        style = ds(30, lineHeight = 1.5, color = c.textSub),
                    )
                }
            }
            // 오른쪽: QR 카드
            Column(
                Modifier
                    .width(680.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(44.dp))
                    .background(c.card)
                    .padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                QrPlaceholder()
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    LineIcon(AppIcon.Waiting, 32.dp, color = c.accent, strokeWidth = 2.4f)
                    Text(
                        when (state.doneSteps) {
                            0 -> "휴대폰에서 로그인하기를 기다리고 있어요"
                            3 -> "Google · Slack 연결을 마쳤어요"
                            else -> "휴대폰에서 연결하고 있어요"
                        },
                        style = ds(30, 600, color = c.accent),
                    )
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(c.background)
                        .padding(horizontal = 28.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("QR이 안 찍히면 휴대폰에서 [주소]에 들어가 이 코드를 입력하세요", style = ds(26, color = c.textSub))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(state.manualCode, style = ds(52, 800, 0.08, tabular = true), modifier = Modifier.alignByBaseline())
                        Text(state.remainingText, style = ds(26, color = c.textSub, tabular = true), modifier = Modifier.alignByBaseline())
                    }
                }
                Spacer(Modifier.weight(1f))
                AppButton(
                    "나중에 할게요", onLater, Modifier.fillMaxWidth(),
                    kind = ButtonKind.Secondary, height = 104.dp, radius = 32.dp, fontSize = 34,
                )
            }
        }
    }
}

@Composable
private fun StepRow(number: Int, label: String, active: Boolean, done: Boolean = false) {
    val c = appColors
    Column {
        if (number > 1) HorizontalDivider(thickness = 1.dp, color = c.divider)
        Row(
            Modifier.padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(if (active || done) c.accent else c.subtle),
                contentAlignment = Alignment.Center,
            ) {
                if (done) LineIcon(AppIcon.Check, 30.dp, color = c.onAccent, strokeWidth = 3f)
                else Text("$number", style = ds(28, 800, color = if (active) c.onAccent else c.text))
            }
            Text(label + if (done) " · 완료" else "", style = ds(36, 600, color = if (done) c.textSub else c.text))
        }
    }
}

/** 실제 QR 대신 자리만 그린 상자. */
@Composable
private fun QrPlaceholder() {
    val c = appColors
    val ink = Color(0xFF1A1A1A)
    Box(
        Modifier
            .size(360.dp)
            .clip(RoundedCornerShape(28.dp))
            .border(2.dp, c.subtle, RoundedCornerShape(28.dp)),
    ) {
        val finder = Modifier.size(104.dp).border(16.dp, ink, RoundedCornerShape(14.dp))
        Box(finder.align(Alignment.TopStart).offset(24.dp, 24.dp))
        Box(finder.align(Alignment.TopEnd).offset((-24).dp, 24.dp))
        Box(finder.align(Alignment.BottomStart).offset(24.dp, (-24).dp))
        Box(
            Modifier
                .offset(130.dp, 130.dp)
                .size(100.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(c.background),
            contentAlignment = Alignment.Center,
        ) { Text("[QR]", style = ds(24, 700, color = c.textSub)) }
    }
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun LoginPreview() = CarProjectTheme {
    DesignCanvas { LoginContent(LoginUiState(manualCode = "7K2-9QF", remainingText = "4:52 남음"), onLater = {}) }
}
