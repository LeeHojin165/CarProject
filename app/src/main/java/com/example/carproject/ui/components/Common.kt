package com.example.carproject.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.carproject.ui.theme.appColors
import com.example.carproject.ui.theme.ds

/** 앱 이름. 정해지면 여기만 바꾼다. */
const val APP_NAME = "[앱 이름]"

/**
 * 주차 중 화면의 공통 틀: padding 64/96/80, 세로 간격 48, 맨 위에 앱 이름과 상태 칩.
 */
@Composable
fun AppScreen(
    statusIcon: AppIcon,
    statusText: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(appColors.background)
            .padding(PaddingValues(start = 96.dp, top = 64.dp, end = 96.dp, bottom = 80.dp)),
        verticalArrangement = Arrangement.spacedBy(48.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(APP_NAME, style = ds(30, 700, -0.01))
            StatusChip(statusIcon, statusText)
        }
        content()
    }
}

/** 오른쪽 위 둥근 상태 칩: padding 14/28, gap 14, 28px/600. */
@Composable
fun StatusChip(icon: AppIcon, text: String, fontSize: Int = 28, iconSize: Dp = 30.dp) {
    val c = appColors
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(c.card)
            .padding(horizontal = 28.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LineIcon(icon, iconSize, color = c.textSub, strokeWidth = 2.2f)
        Text(text, style = ds(fontSize, 600, color = c.textSub))
    }
}

enum class ButtonKind { Primary, Secondary }

/**
 * HTML의 button. 운전 중에도 누르기 쉽게 높이는 96dp 이상만 쓴다.
 * Primary: 강조색 배경, Secondary: 보조색 배경.
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Primary,
    height: Dp = 144.dp,
    radius: Dp = 36.dp,
    fontSize: Int = if (kind == ButtonKind.Primary) 46 else 38,
    weight: Int = if (kind == ButtonKind.Primary) 700 else 600,
    tracking: Double = if (kind == ButtonKind.Primary) -0.01 else 0.0,
    horizontalPadding: Dp = 0.dp,
) {
    require(height >= 96.dp) { "버튼 높이는 최소 96dp" }
    val c = appColors
    val (bg, fg) = when (kind) {
        ButtonKind.Primary -> c.accent to c.onAccent
        ButtonKind.Secondary -> c.subtle to c.text
    }
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(radius))
            .background(bg)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = ds(fontSize, weight, tracking, color = fg), textAlign = TextAlign.Center)
    }
}

/** 오른쪽 카드 제목: 36px/700, 아래 여백 12. */
@Composable
fun CardTitle(text: String) {
    Text(text, style = ds(36, 700, -0.01), modifier = Modifier.padding(bottom = 12.dp))
}

/**
 * 카드 안 점검 항목 한 줄: 아이콘 44 · 이름 32px · 값 34px/700. 첫 줄이 아니면 위에 구분선.
 */
@Composable
fun CheckRow(
    icon: AppIcon,
    label: String,
    value: String,
    showDivider: Boolean,
    warning: Boolean = false,
    verticalPadding: Dp = 28.dp,
) {
    val c = appColors
    val tone: Color = if (warning) c.warning else c.textSub
    Column {
        if (showDivider) HorizontalDivider(thickness = 1.dp, color = c.divider)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = verticalPadding),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LineIcon(icon, 44.dp, color = tone, strokeWidth = 2f)
            Text(label, style = ds(32, color = tone), modifier = Modifier.weight(1f))
            Text(value, style = ds(34, 700, color = if (warning) c.warning else c.text))
        }
    }
}
