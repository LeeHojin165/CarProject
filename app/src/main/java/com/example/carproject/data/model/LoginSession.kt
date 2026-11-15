package com.example.carproject.data.model

/** QR 로그인 대기 상태. */
data class LoginSession(
    val manualCode: String,
    val remainingSeconds: Int,
    val profileName: String,
    /** 휴대폰에서 끝낸 단계 수. 0: 기다리는 중, 1: QR 찍음, 2: 구글 로그인, 3: Slack 연결. */
    val doneSteps: Int = 0,
    val isLoggedIn: Boolean = false,
)
