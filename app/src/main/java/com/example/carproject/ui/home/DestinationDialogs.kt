package com.example.carproject.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.carproject.data.model.Place
import com.example.carproject.data.model.RecentPlace
import com.example.carproject.ui.components.AppButton
import com.example.carproject.ui.components.AppIcon
import com.example.carproject.ui.components.ButtonKind
import com.example.carproject.ui.components.LineIcon
import com.example.carproject.ui.components.VoicePromptBox
import com.example.carproject.ui.theme.appColors
import com.example.carproject.ui.theme.ds
import kotlin.math.PI
import kotlin.math.sin

/**
 * 화면 위 어두운 막 + 가운데 대화상자: 너비 1080, padding 64/72, 모서리 52, 세로 간격 36.
 * 막을 누르면 닫힌다.
 */
@Composable
fun ModalDialogFrame(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = appColors
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x7A1A1A1A))
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .width(1080.dp)
                .shadow(64.dp, RoundedCornerShape(52.dp), ambientColor = Color(0x38000000), spotColor = Color(0x38000000))
                .clip(RoundedCornerShape(52.dp))
                .background(c.card)
                // 대화상자 안을 눌러도 닫히지 않게 클릭을 먹는다.
                .clickable(remember { MutableInteractionSource() }, indication = null) {}
                .padding(horizontal = 72.dp, vertical = 64.dp),
            verticalArrangement = Arrangement.spacedBy(36.dp),
            content = content,
        )
    }
}

/** 작은 머리글 + 큰 제목. HTML은 제목에 margin-top -20이라 둘 사이 간격이 16이다. */
@Composable
private fun DialogHeading(eyebrow: String, title: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(eyebrow, style = ds(30, 600, color = appColors.textSub))
        title()
    }
}

/** design/02_DestConfirm.html */
@Composable
fun ConfirmWorkDialog(
    dialog: HomeDialog.ConfirmWork,
    onGoToWork: (Place) -> Unit,
    onOtherPlace: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = appColors
    ModalDialogFrame(onDismiss) {
        DialogHeading("출근 경로를 잡기 전에") {
            Text(
                buildAnnotatedString {
                    append("오늘도 ")
                    withStyle(SpanStyle(color = c.accent)) { append("회사") }
                    append("로 가시나요?")
                },
                style = ds(72, 800, -0.03, 1.2),
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(c.background)
                .padding(horizontal = 36.dp, vertical = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(88.dp).clip(RoundedCornerShape(26.dp)).background(c.card),
                contentAlignment = Alignment.Center,
            ) { LineIcon(AppIcon.Building, 48.dp, color = c.accent, strokeWidth = 2f) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(dialog.workplace.name, style = ds(40, 800))
                Text("${dialog.workplace.address} · 마이페이지에 저장한 곳", style = ds(28, color = c.textSub))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("지금 출발하면", style = ds(26, color = c.textSub))
                Text(dialog.arrivalText, style = ds(40, 800, color = c.accent, tabular = true))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            AppButton(
                "네, 회사로 가요", { onGoToWork(dialog.workplace) }, Modifier.weight(1.3f),
                height = 136.dp, fontSize = 44, tracking = 0.0,
            )
            AppButton(
                "다른 곳이에요", onOtherPlace, Modifier.weight(1f),
                kind = ButtonKind.Secondary, height = 136.dp, fontSize = 40,
            )
        }
        Text(
            "퇴근이에요를 누르면 같은 방식으로 집이 맞는지 물어봐요.",
            style = ds(28, lineHeight = 1.5, color = c.textSub),
        )
    }
}

/** design/03_DestAsk.html */
@Composable
fun AskDestinationDialog(
    dialog: HomeDialog.AskDestination,
    onPlaceChosen: (Place) -> Unit,
    onBackToWork: () -> Unit,
    onRetryVoice: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = appColors
    val voice = dialog.voice
    ModalDialogFrame(onDismiss) {
        DialogHeading("회사가 아니군요") {
            Text("어디로 갈까요?", style = ds(72, 800, -0.03, 1.2))
        }
        VoicePromptBox(
            voice = voice,
            example = "판교 [거래처] 사옥으로 가줘",
            confirmedText = "이곳으로 안내할게요",
            onRetry = onRetryVoice,
        )
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("최근에 간 곳", style = ds(28, 700, color = c.textSub))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                dialog.recentPlaces.forEach { recent ->
                    RecentPlaceButton(recent, Modifier.weight(1f)) { onPlaceChosen(recent.place) }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("고르면 그곳으로 내비게이션을 바로 열어요.", style = ds(28, color = c.textSub))
            AppButton(
                "회사로 갈게요", onBackToWork,
                kind = ButtonKind.Secondary, height = 96.dp, radius = 28.dp, fontSize = 32,
                horizontalPadding = 40.dp,
            )
        }
    }
}

@Composable
private fun RecentPlaceButton(
    recent: RecentPlace,
    modifier: Modifier,
    height: Dp = 120.dp,
    radius: Dp = 32.dp,
    titleSize: Int = 34,
    onClick: () -> Unit,
) {
    val c = appColors
    Column(
        modifier
            .height(height)
            .clip(RoundedCornerShape(radius))
            .background(c.background)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 36.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        Text(recent.place.name, style = ds(titleSize, 700), maxLines = 1)
        Text(recent.lastVisitedLabel, style = ds(24, color = c.textSub), maxLines = 1)
    }
}

/** design/18_TripDest.html: 어디로 출장 가세요? */
@Composable
fun TripDestDialog(
    dialog: HomeDialog.AskTrip,
    onPlaceChosen: (Place) -> Unit,
    onRetryVoice: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = appColors
    ModalDialogFrame(onDismiss) {
        DialogHeading("출장지는 매번 달라서 먼저 여쭤볼게요") {
            Text("어디로 출장 가세요?", style = ds(72, 800, -0.03, 1.2))
        }
        dialog.suggestion?.let { sug ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(c.background)
                    .padding(horizontal = 32.dp, vertical = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(88.dp).clip(RoundedCornerShape(26.dp)).background(c.card),
                    contentAlignment = Alignment.Center,
                ) { LineIcon(AppIcon.Calendar, 48.dp, color = c.accent, strokeWidth = 2f) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "오늘 일정에서 찾았어요 · %d:%02d ${sug.event.title}".format(sug.event.time.hour, sug.event.time.minute),
                        style = ds(26, 700, color = c.accent),
                    )
                    Text(sug.place.name, style = ds(40, 800))
                    Text("${sug.place.address} · ${sug.arrivalText} 도착 예정", style = ds(26, color = c.textSub))
                }
                AppButton(
                    "여기로 가요", { onPlaceChosen(sug.place) },
                    height = 112.dp, radius = 32.dp, fontSize = 36, tracking = 0.0, horizontalPadding = 40.dp,
                )
            }
        }
        VoicePromptBox(
            voice = dialog.voice,
            example = "[고객사] 수원 공장으로 가줘",
            confirmedText = "이곳으로 안내할게요",
            onRetry = onRetryVoice,
            title = "다른 곳이면 말씀해 주세요",
            barHeights = listOf(18, 36, 52, 30, 44, 22),
            titleSize = 32,
            subSize = 24,
            paddingH = 32.dp,
            paddingV = 24.dp,
            barWidth = 8.dp,
            barGap = 7.dp,
            barAreaHeight = 56.dp,
        )
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("최근 출장지", style = ds(28, 700, color = c.textSub))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                dialog.recentPlaces.forEach { recent ->
                    RecentPlaceButton(recent, Modifier.weight(1f), height = 104.dp, radius = 30.dp, titleSize = 32) {
                        onPlaceChosen(recent.place)
                    }
                }
            }
        }
        Text("고르면 그곳으로 내비게이션을 열고, 출장 화면에 가서 할 일을 띄워요.", style = ds(26, color = c.textSub))
    }
}
