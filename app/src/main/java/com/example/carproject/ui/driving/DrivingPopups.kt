package com.example.carproject.ui.driving

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.carproject.driving.DrivingPopup
import com.example.carproject.ui.components.APP_NAME
import com.example.carproject.ui.components.AppButton
import com.example.carproject.ui.components.AppIcon
import com.example.carproject.ui.components.ButtonKind
import com.example.carproject.ui.components.LineIcon
import com.example.carproject.ui.theme.CarProjectTheme
import com.example.carproject.ui.theme.appColors
import com.example.carproject.ui.theme.ds

private val ShadowColor = Color(0x2E12171E)

/** 주행 중 팝업 하나를 그린다. 위치는 띄우는 쪽(오버레이 창)이 정한다. */
@Composable
fun DrivingPopupContent(popup: DrivingPopup, onSend: () -> Unit, onDismiss: () -> Unit) {
    when (popup) {
        DrivingPopup.None -> Unit
        is DrivingPopup.LateNotice -> LateNoticeCard(popup, onSend, onDismiss)
        is DrivingPopup.Sent -> SentPill(popup)
    }
}

/** design/05_LateNotice.html: 너비 1160, padding 36/44, 모서리 40, 세로 간격 24. */
@Composable
fun LateNoticeCard(notice: DrivingPopup.LateNotice, onSend: () -> Unit, onDismiss: () -> Unit) {
    val c = appColors
    Column(
        Modifier
            .width(1160.dp)
            .shadow(48.dp, RoundedCornerShape(40.dp), ambientColor = ShadowColor, spotColor = ShadowColor)
            .clip(RoundedCornerShape(40.dp))
            .background(c.card)
            .padding(horizontal = 44.dp, vertical = 36.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(c.accent))
            Text(APP_NAME, style = ds(26, 600, color = c.textSub))
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = c.warning)) { append("${notice.lateMinutes}분") }
                    append(" 늦을 것 같아요. 팀에 알릴까요?")
                },
                style = ds(56, 800, -0.02, 1.25),
            )
            Text(
                "#${notice.channel}에 \"${notice.message}\"라고 보낼게요.",
                style = ds(30, lineHeight = 1.5, color = c.textSub),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            AppButton("보내기", onSend, Modifier.weight(1f), height = 112.dp, radius = 32.dp, fontSize = 44, tracking = 0.0)
            AppButton(
                "괜찮아요", onDismiss, Modifier.weight(1f),
                kind = ButtonKind.Secondary, height = 112.dp, radius = 32.dp, fontSize = 40,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            LineIcon(AppIcon.Mic, 30.dp, color = c.textSub, strokeWidth = 2.2f)
            Text("\"보내줘\"라고 말해도 돼요. 답이 없으면 보내지 않고 닫혀요.", style = ds(26, color = c.textSub))
        }
    }
}

/** design/06_DrivingCalm.html: 둥근 알약 모양, padding 24/36/24/28, 간격 20. */
@Composable
fun SentPill(sent: DrivingPopup.Sent) {
    val c = appColors
    Row(
        Modifier
            .shadow(36.dp, RoundedCornerShape(999.dp), ambientColor = ShadowColor, spotColor = ShadowColor)
            .clip(RoundedCornerShape(999.dp))
            .background(c.card)
            .padding(start = 28.dp, end = 36.dp, top = 24.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LineIcon(AppIcon.CheckCircle, 48.dp, color = c.accent, strokeWidth = 2.4f)
        Text("팀 채널에 알렸어요", style = ds(36, 700))
        Text("· ${sent.arrivalText} 도착 예정", style = ds(32, color = c.textSub, tabular = true))
    }
}

@Preview(widthDp = 700, heightDp = 400, showBackground = true, backgroundColor = 0xFFEDEBE8)
@Composable
private fun LateNoticePreview() = CarProjectTheme {
    Box(Modifier.padding(20.dp)) {
        LateNoticeCard(DrivingPopup.LateNotice(8, "[팀 채널]", "출근길 정체로 8분 정도 늦습니다", "9:08"), {}, {})
    }
}
