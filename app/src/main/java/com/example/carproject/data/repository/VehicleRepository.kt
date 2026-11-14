package com.example.carproject.data.repository

import com.example.carproject.data.model.Seat
import com.example.carproject.data.model.VehicleStatus
import kotlinx.coroutines.flow.StateFlow

interface VehicleRepository {
    val status: StateFlow<VehicleStatus>

    /** 창문을 닫는다. 실행 전에 반드시 사용자에게 물어야 한다(CLAUDE.md). */
    suspend fun closeWindow(seat: Seat): Boolean

    /** 내린 동안 공조·열선을 켜 둔다. 실행 전에 반드시 묻는다. */
    suspend fun keepClimate(tempC: Int, seatHeatLevel: Int): Boolean

    /** 앞좌석 창문을 조금 열었다가 seconds 뒤 닫는다. 실행 전에 반드시 묻는다. */
    suspend fun ventilate(seconds: Int): Boolean
}
