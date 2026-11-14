package com.example.carproject.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.carproject.ui.theme.appColors
import com.example.carproject.ui.theme.ds
import kotlin.math.PI
import kotlin.math.sin

/** 음성 입력 상자 상태. */
data class VoiceInput(
    val listening: Boolean = false,
    val transcript: String? = null,
    val error: String? = null,
    /** 확정되어 곧 다음으로 넘어간다. */
    val confirmed: Boolean = false,
)

/**
 * "말씀해 주세요" 음성 입력 상자. 03 대화상자와 07 주차 화면이 함께 쓴다.
 * 듣고 있지 않을 때 누르면 다시 듣는다.
 */
@Composable
fun VoicePromptBox(
    voice: VoiceInput,
    example: String,
    confirmedText: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    barHeights: List<Int> = listOf(24, 48, 72, 40, 60, 28, 52, 36),
    titleSize: Int = 38,
    subSize: Int = 26,
    radius: Dp = 32.dp,
    background: Color = Color.Transparent,
    title: String = "말씀해 주세요",
    paddingH: Dp = 40.dp,
    paddingV: Dp = 32.dp,
    barWidth: Dp = 10.dp,
    barGap: Dp = 8.dp,
    barAreaHeight: Dp = 72.dp,
) {
    val c = appColors
    val shape = RoundedCornerShape(radius)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .border(3.dp, if (voice.error != null) c.warning else c.accent, shape)
            .clickable(enabled = !voice.listening && !voice.confirmed, role = Role.Button, onClick = onRetry)
            .padding(horizontal = paddingH, vertical = paddingV),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (voice.error != null) {
            Box(Modifier.size(width = (barHeights.size * 18).dp, height = 72.dp), contentAlignment = Alignment.Center) {
                LineIcon(AppIcon.Mic, 56.dp, color = c.warning, strokeWidth = 2.2f)
            }
        } else {
            ListeningBars(barHeights, animate = voice.listening, barWidth, barGap, barAreaHeight)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                voice.transcript?.let { "“$it”" } ?: title,
                style = ds(titleSize, 700), maxLines = 1,
            )
            Text(
                when {
                    voice.confirmed -> confirmedText
                    voice.error != null -> "${voice.error} · 눌러서 다시 말하기"
                    voice.listening -> "예: \"$example\" · 글레오가 듣고 있어요"
                    else -> "눌러서 다시 말하기"
                },
                style = ds(subSize, color = if (voice.error != null) c.warning else c.textSub),
            )
        }
    }
}

/** 듣고 있음을 보여주는 막대. 디자인의 높이를 기준으로 천천히 오르내린다. */
@Composable
private fun ListeningBars(base: List<Int>, animate: Boolean, barWidth: Dp, barGap: Dp, areaHeight: Dp) {
    val c = appColors
    val phase by rememberInfiniteTransition(label = "listening").animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    Row(
        Modifier.height(areaHeight),
        horizontalArrangement = Arrangement.spacedBy(barGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        base.forEachIndexed { i, h ->
            val k = if (animate) 0.75f + 0.25f * sin(phase + i * 0.9f) else 1f
            Box(
                Modifier
                    .width(barWidth)
                    .height((h * k).dp)
                    .clip(RoundedCornerShape(barWidth / 2))
                    .background(c.accent),
            )
        }
    }
}
