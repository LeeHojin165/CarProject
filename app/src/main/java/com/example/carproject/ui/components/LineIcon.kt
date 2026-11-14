package com.example.carproject.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import com.example.carproject.ui.theme.appColors

/**
 * design/ HTML에 들어 있는 24×24 선 아이콘을 SVG path 문자열 그대로 옮긴 것.
 * circle·rect는 [circle]·[rect]로 path로 바꿔 넣는다.
 */
enum class AppIcon(vararg val paths: String) {
    Person(circle(12f, 8f, 4f), "M4.5 20.5a7.5 7.5 0 0 1 15 0"),
    PersonWide(circle(12f, 8f, 4f), "M4 21a8 8 0 0 1 16 0"),
    Lock(rect(5f, 10.5f, 14f, 10f, 2.5f), "M8 10.5V7.5a4 4 0 0 1 8 0v3"),
    Waiting("M5 18V8a3 3 0 0 1 3-3h8a3 3 0 0 1 3 3v10", "M3 18h18", "M10 15h4"),
    Parking(rect(4f, 4f, 16f, 16f, 4f), "M10 16V8h3a2.5 2.5 0 0 1 0 5h-3"),
    Battery(rect(3f, 8f, 16f, 9f, 2.5f), "M22 11v3", "M7 11.5v2M10.5 11.5v2"),
    Tire(circle(12f, 12f, 9f), circle(12f, 12f, 3.5f)),
    Warning("M12 3 2.5 20h19L12 3z", "M12 10v4.5M12 17.2v.3"),
    Drop("M12 3.5c3 4 5.5 7 5.5 10.5a5.5 5.5 0 0 1-11 0C6.5 10.5 9 7.5 12 3.5z"),
    Window("M4 18V11l5-6h9a2 2 0 0 1 2 2v11H4z", "M4 12h16"),
    Bulb("M9 18h6M10 21h4", "M12 3a6 6 0 0 0-3.5 10.9c.6.5 1 1.2 1 2.1h5c0-.9.4-1.6 1-2.1A6 6 0 0 0 12 3z"),
    Chat("M4 5.5h16v11H9.5L5 20v-3.5H4v-11z"),
    Mic(rect(9f, 3f, 6f, 11f, 3f), "M5.5 11.5a6.5 6.5 0 0 0 13 0M12 18v3"),
    TurnRight("M7 20V11a3 3 0 0 1 3-3h8", "M15 4.5 18.5 8 15 11.5"),
    Close("M6 6l12 12M18 6L6 18"),
    Speaker("M4 9.5v5h3.5L12 18.5v-13L7.5 9.5H4z", "M16 9a4.5 4.5 0 0 1 0 6M18.8 6.5a8 8 0 0 1 0 11"),
    Pin("M12 21s-6.5-5.6-6.5-11a6.5 6.5 0 0 1 13 0c0 5.4-6.5 11-6.5 11z", circle(12f, 10f, 2.3f)),
    Briefcase(rect(3.5f, 7f, 17f, 12.5f, 2.5f), "M9 7V5.5A1.5 1.5 0 0 1 10.5 4h3A1.5 1.5 0 0 1 15 5.5V7"),
    PinBold("M12 21s-7-6.2-7-11.5a7 7 0 0 1 14 0C19 14.8 12 21 12 21z", circle(12f, 9.5f, 2.5f)),
    CheckCircle(circle(12f, 12f, 9f), "M8 12.3l2.8 2.8L16 9.5"),
    Back("M15 5l-7 7 7 7"),
    Image(rect(3f, 5f, 18f, 14f, 2.5f), circle(8.5f, 10f, 1.6f), "M21 16l-5-5-9 8"),
    Thermometer("M10 13.5V5a2 2 0 0 1 4 0v8.5a4 4 0 1 1-4 0z"),
    SeatHeat("M5 20v-6a3 3 0 0 1 3-3h5l4 4v5", "M9 4c-1 1.3 1 2.2 0 3.5M13 4c-1 1.3 1 2.2 0 3.5"),
    Timer(circle(12f, 13f, 8f), "M12 9v4l2.5 2M10 2h4"),
    Leaf("M12 21v-8", "M12 13c0-4 2.5-7 7-7 0 4.5-3 7-7 7z", "M12 15c0-3-2-5.5-6-5.5 0 3.5 2.5 5.5 6 5.5z"),
    Building("M3 21V9l6-4v16M9 21h12V11l-6-3", "M13 13h4M13 17h4"),
}

fun circle(cx: Float, cy: Float, r: Float): String =
    "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0z"

fun rect(x: Float, y: Float, w: Float, h: Float, rx: Float = 0f): String =
    if (rx == 0f) "M$x ${y}h${w}v${h}h${-w}z"
    else "M${x + rx} ${y}h${w - 2 * rx}a$rx $rx 0 0 1 $rx ${rx}v${h - 2 * rx}" +
        "a$rx $rx 0 0 1 ${-rx} ${rx}h${-(w - 2 * rx)}a$rx $rx 0 0 1 ${-rx} ${-rx}" +
        "v${-(h - 2 * rx)}a$rx $rx 0 0 1 $rx ${-rx}z"

/**
 * @param size HTML의 width/height 값
 * @param strokeWidth HTML의 stroke-width 값 (24 viewBox 기준)
 */
@Composable
fun LineIcon(
    icon: AppIcon,
    size: Dp,
    modifier: Modifier = Modifier,
    color: Color = appColors.text,
    strokeWidth: Float = 2f,
) {
    val paths: List<Path> = remember(icon) {
        icon.paths.map { PathParser().parsePathString(it).toPath() }
    }
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 24f
        scale(s, pivot = Offset.Zero) {
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            paths.forEach { drawPath(it, color, style = stroke) }
        }
    }
}
