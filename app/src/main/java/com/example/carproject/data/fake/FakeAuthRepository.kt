package com.example.carproject.data.fake

import com.example.carproject.data.model.LoginSession
import com.example.carproject.data.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 가짜 QR 로그인. 실제 휴대폰이 없으니 시연 메뉴(또는 adb --es login phone)로
 * "휴대폰에서 QR을 찍었다"를 알리면 QR → 구글 → Slack 순서로 2초마다 한 단계씩 끝난다.
 */
class FakeAuthRepository : AuthRepository {
    @Volatile var phoneScanned = false

    override fun startQrLogin(): Flow<LoginSession> = flow {
        var left = 4 * 60 + 52
        var ticks = 0
        while (left >= 0) {
            if (phoneScanned) ticks++
            val done = if (!phoneScanned) 0 else minOf(3, 1 + (ticks - 1) / 2)
            emit(
                LoginSession(
                    manualCode = "7K2-9QF", remainingSeconds = left, profileName = "[프로필 이름]",
                    doneSteps = done, isLoggedIn = done == 3 && ticks >= 7,
                ),
            )
            delay(1_000)
            left--
        }
    }
}
