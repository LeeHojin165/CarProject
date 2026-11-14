package com.example.carproject.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * design/ HTML에서 그대로 옮긴 색.
 * 출근(라이트)·퇴근(다크) 화면이 같은 이름의 토큰을 쓰도록 맞춘다.
 */
@Immutable
data class AppColors(
    val background: Color,     // 화면 배경
    val card: Color,           // 카드, 칩
    val subtle: Color,         // 보조 버튼, 카드 안 상자
    val divider: Color,        // 카드 안 구분선
    val text: Color,
    val textSub: Color,
    val accent: Color,         // 주 버튼 배경, 강조 글자
    val onAccent: Color,       // 주 버튼 글자
    val link: Color,
    val warning: Color,        // 부족·열림 같은 주의 표시
    val isDark: Boolean,
)

val LightAppColors = AppColors(
    background = Color(0xFFF6F3F2),
    card = Color(0xFFFFFFFF),
    subtle = Color(0xFFE2DFDB),
    divider = Color(0xFFE2DFDB),
    text = Color(0xFF1A1A1A),
    textSub = Color(0xFF5C5955),
    accent = Color(0xFF002C5F),
    onAccent = Color(0xFFFFFFFF),
    link = Color(0xFF002C5F),
    warning = Color(0xFFA36B4F),
    isDark = false,
)

val DarkAppColors = AppColors(
    background = Color(0xFF0A0F17),
    card = Color(0xFF121A26),
    subtle = Color(0xFF1C2636),
    divider = Color(0xFF2A3546),
    text = Color(0xFFF6F3F2),
    textSub = Color(0xFFA7B0BD),
    accent = Color(0xFFAACAE6),
    onAccent = Color(0xFF002C5F),
    link = Color(0xFF00AAD2),
    warning = Color(0xFFE0A88A),
    isDark = true,
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }
