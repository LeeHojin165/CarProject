package com.example.carproject.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/** 출근(라이트) 화면인지 퇴근(다크) 화면인지. 시스템 다크 모드와 무관하게 화면마다 정한다. */
enum class CommuteMode { Morning, Evening }

/** 디자인 기준 크기. HTML의 1920×1080 px. */
const val DESIGN_WIDTH = 1920
const val DESIGN_HEIGHT = 1080

@Composable
fun CarProjectTheme(
    mode: CommuteMode = CommuteMode.Morning,
    content: @Composable () -> Unit,
) {
    val colors = if (mode == CommuteMode.Evening) DarkAppColors else LightAppColors
    val scheme = if (colors.isDark) {
        darkColorScheme(
            primary = colors.accent, onPrimary = colors.onAccent,
            background = colors.background, onBackground = colors.text,
            surface = colors.card, onSurface = colors.text,
        )
    } else {
        lightColorScheme(
            primary = colors.accent, onPrimary = colors.onAccent,
            background = colors.background, onBackground = colors.text,
            surface = colors.card, onSurface = colors.text,
        )
    }
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(
            LocalAppColors provides colors,
            // 글자 스타일에 색을 따로 주지 않은 Text는 이 색을 쓴다. 다크 화면에서 검은 글자가 나오지 않게 한다.
            LocalContentColor provides colors.text,
            LocalTextStyle provides ds(32, color = colors.text),
            content = content,
        )
    }
}

/** 지금 화면의 색. */
val appColors: AppColors
    @Composable get() = LocalAppColors.current

/**
 * 1920×1080 디자인 캔버스를 실제 앱 영역에 맞춘다. 안쪽에서는 HTML의 px 값을 그대로 dp/sp로 쓰면 된다.
 * Pleos Connect의 앱 영역은 약 1680×1210(16:9보다 세로가 긺)이라, 짧은 쪽 기준으로 배율을 정하고
 * 남는 방향으로 캔버스를 늘려 빈 띠 없이 채운다. 화면 레이아웃은 weight로 짜서 늘어난 만큼 따라온다.
 */
@Composable
fun DesignCanvas(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(appColors.background),
        contentAlignment = Alignment.Center,
    ) {
        val scale = minOf(
            constraints.maxWidth / DESIGN_WIDTH.toFloat(),
            constraints.maxHeight / DESIGN_HEIGHT.toFloat(),
        )
        val canvasW = constraints.maxWidth / scale
        val canvasH = constraints.maxHeight / scale
        CompositionLocalProvider(LocalDensity provides Density(density = scale, fontScale = 1f)) {
            Box(Modifier.requiredSize(canvasW.dp, canvasH.dp)) {
                content()
            }
        }
    }
}
