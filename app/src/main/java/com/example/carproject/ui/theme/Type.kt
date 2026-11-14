package com.example.carproject.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.carproject.R

/** res/font/asta_sans.ttf는 가변 폰트(wght 400~800)라 굵기마다 축 값을 지정한다. */
@OptIn(ExperimentalTextApi::class)
val AstaSans = FontFamily(
    listOf(400, 500, 600, 700, 800).map { w ->
        Font(
            R.font.asta_sans,
            weight = FontWeight(w),
            variationSettings = FontVariation.Settings(FontVariation.weight(w)),
        )
    }
)

/**
 * HTML 값을 그대로 옮기기 위한 글자 스타일.
 * size: px 값 그대로, weight: 400~800, tracking: letter-spacing(em), lineHeight: line-height 배수.
 */
fun ds(
    size: Int,
    weight: Int = 400,
    tracking: Double = 0.0,
    lineHeight: Double? = null,
    color: Color = Color.Unspecified,
    tabular: Boolean = false,
): TextStyle = TextStyle(
    fontFamily = AstaSans,
    fontSize = size.sp,
    fontWeight = FontWeight(weight),
    letterSpacing = tracking.em,
    lineHeight = lineHeight?.em ?: TextStyle.Default.lineHeight,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    ),
    color = color,
    fontFeatureSettings = if (tabular) "tnum" else null,
)
