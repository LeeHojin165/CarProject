package com.example.carproject.ui.leave

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.carproject.ui.components.APP_NAME
import com.example.carproject.ui.theme.DESIGN_HEIGHT
import com.example.carproject.ui.theme.DESIGN_WIDTH
import com.example.carproject.ui.theme.appColors
import com.example.carproject.ui.theme.ds

/** design/12~14의 지도 그림 색. 실제 지도가 아니라 장면을 보여주기 위한 가짜 지도다. */
private val MapBg = Color(0xFF0D131C)
private val Road = Color(0xFF1F2938)
private val Block = Color(0xFF172130)
private val BlockAlt = Color(0xFF16222E)
private val RouteColor = Color(0xFF00AAD2)
private val Pin = Color(0xFFF6F3F2)
private val Ink = Color(0xFF0A0F17)

/**
 * 회사→집 경로가 그려진 가짜 지도 + 그 위에 놓는 표시들.
 * 좌표는 HTML의 1920×1080 기준이고, 캔버스가 세로로 길어지면 그만큼 늘린다.
 */
@Composable
fun FakeRouteMap(
    showWorkLabel: Boolean = true,
    overlays: @Composable MapScope.() -> Unit = {},
) {
    BoxWithConstraints(Modifier.fillMaxSize().background(MapBg)) {
        Canvas(Modifier.fillMaxSize()) {
            scale(size.width / DESIGN_WIDTH, size.height / DESIGN_HEIGHT, pivot = Offset.Zero) {
                fun line(x1: Float, y1: Float, x2: Float, y2: Float, w: Float) =
                    drawLine(Road, Offset(x1, y1), Offset(x2, y2), strokeWidth = w)
                line(760f, 880f, 1920f, 760f, 44f)
                line(760f, 420f, 1920f, 520f, 30f)
                line(1120f, 1080f, 1220f, 0f, 38f)
                line(1540f, 1080f, 1480f, 0f, 30f)
                fun block(x: Float, y: Float, w: Float, h: Float, c: Color = Block) =
                    drawRoundRect(c, Offset(x, y), Size(w, h), CornerRadius(14f))
                block(1260f, 560f, 190f, 160f)
                block(880f, 560f, 200f, 130f)
                block(1560f, 560f, 260f, 150f, BlockAlt)
                block(1260f, 120f, 180f, 250f)
                val route = Path().apply {
                    moveTo(900f, 1000f)
                    cubicTo(1000f, 860f, 1140f, 820f, 1240f, 700f)
                    cubicTo(1340f, 580f, 1460f, 420f, 1600f, 300f)
                    cubicTo(1740f, 180f, 1760f, 150f, 1820f, 110f)
                }
                drawPath(route, RouteColor, style = Stroke(16f, cap = StrokeCap.Round))
                drawCircle(Pin, 18f, Offset(900f, 1000f))
                drawCircle(Pin, 18f, Offset(1820f, 110f))
            }
        }
        val scope = MapScope(this)
        if (showWorkLabel) scope.At(932f, 984f) { Text("회사", style = ds(24, 600, color = appColors.textSub)) }
        scope.At(1760f, 140f) { Text("집", style = ds(24, 600, color = appColors.textSub)) }
        scope.overlays()
    }
}

class MapScope(private val box: BoxWithConstraintsScope) {
    /** HTML 좌표(1920×1080 기준)에 놓는다. */
    @Composable
    fun At(x: Float, y: Float, content: @Composable () -> Unit) {
        val sy = box.maxHeight.value / DESIGN_HEIGHT
        Box(Modifier.offset(x.dp, (y * sy).dp)) { content() }
    }
}

/** 지도 위 번호 알약. 고른 것은 강조색. */
@Composable
fun NumberedMapPill(number: Int, label: String, selected: Boolean) {
    val bg = if (selected) appColors.accent else Pin
    val fg = if (selected) appColors.onAccent else Ink
    Row(
        Modifier
            .shadow(24.dp, CircleShape, ambientColor = Color(0x66000000), spotColor = Color(0x66000000))
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(start = 12.dp, end = 22.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NumberBadge(number, 44.dp, 26, bg = fg, fg = bg)
        Text(label, style = ds(28, 700, color = fg, tabular = true))
    }
}

@Composable
fun NumberBadge(number: Int, size: Dp, fontSize: Int, bg: Color, fg: Color) {
    Box(Modifier.size(size).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        Text("$number", style = ds(fontSize, 800, color = fg))
    }
}

/** 지도 왼쪽에 떠 있는 패널: left/top/bottom 48, 모서리 44, 배경색. */
@Composable
fun MapSidePanel(width: Dp, content: @Composable ColumnScope.() -> Unit) {
    val c = appColors
    Column(
        Modifier
            .padding(48.dp)
            .fillMaxHeight()
            .width(width)
            .shadow(48.dp, RoundedCornerShape(44.dp), ambientColor = Color(0x80000000), spotColor = Color(0x80000000))
            .clip(RoundedCornerShape(44.dp))
            .background(c.background)
            .padding(start = 48.dp, end = 48.dp, top = 48.dp, bottom = 44.dp),
        content = content,
    )
}

/** 패널 맨 위: 앱 이름 + 작은 상태 칩. */
@Composable
fun PanelHeader(status: String) {
    val c = appColors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(APP_NAME, style = ds(28, 700), modifier = Modifier.weight(1f))
        Text(
            status,
            style = ds(24, 600, color = c.textSub),
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(c.card)
                .padding(horizontal = 22.dp, vertical = 10.dp),
        )
    }
}

/** 12·13의 번호 목록 한 줄. 고른 줄은 보조색 상자로 감싼다. */
@Composable
fun NumberedListItem(
    number: Int,
    title: String,
    trailing: String,
    trailingColor: Color,
    sub: String,
    subTrailing: String?,
    selected: Boolean,
    showDivider: Boolean,
    verticalPadding: Dp,
    onClick: () -> Unit,
) {
    val c = appColors
    Column {
        if (showDivider) androidx.compose.material3.HorizontalDivider(thickness = 1.dp, color = c.divider)
        Column(
            Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(if (selected) c.subtle else Color.Transparent)
                .androidxClickable(onClick)
                .padding(horizontal = 24.dp, vertical = verticalPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                NumberBadge(number, 40.dp, 24, bg = if (selected) c.accent else Pin, fg = if (selected) c.onAccent else Ink)
                Text(title, style = ds(32, 700), modifier = Modifier.weight(1f), maxLines = 1)
                Text(trailing, style = ds(26, 700, color = trailingColor))
            }
            Row(Modifier.padding(start = 54.dp)) {
                Text(sub, style = ds(26, color = c.textSub, tabular = true), modifier = Modifier.weight(1f))
                subTrailing?.let { Text(it, style = ds(26, color = c.textSub, tabular = true)) }
            }
        }
    }
}

private fun Modifier.androidxClickable(onClick: () -> Unit) =
    this.then(Modifier.clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick))
