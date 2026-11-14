package com.example.carproject.data.model

/** QR 로그인 대기 상태. */
data class LoginSession(
    val manualCode: String,
    val remainingSeconds: Int,
    val profileName: String,
    val isLoggedIn: Boolean = false,
)
