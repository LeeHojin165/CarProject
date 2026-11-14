package com.example.carproject.data.repository

import com.example.carproject.data.model.LoginSession
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** QR 로그인을 시작하고, 남은 시간과 로그인 완료 여부를 흘려보낸다. */
    fun startQrLogin(): Flow<LoginSession>
}
