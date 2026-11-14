package com.example.carproject.data.fake

import com.example.carproject.data.model.LoginSession
import com.example.carproject.data.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeAuthRepository : AuthRepository {
    override fun startQrLogin(): Flow<LoginSession> = flow {
        var left = 4 * 60 + 52
        while (left >= 0) {
            emit(LoginSession(manualCode = "7K2-9QF", remainingSeconds = left, profileName = "[프로필 이름]"))
            delay(1_000)
            left--
        }
    }
}
