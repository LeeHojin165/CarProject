package com.example.carproject.data.repository

import com.example.carproject.data.model.Seat
import com.example.carproject.data.model.VehicleStatus
import kotlinx.coroutines.flow.StateFlow

interface VehicleRepository {
    val status: StateFlow<VehicleStatus>

    /** 창문을 닫는다. 실행 전에 반드시 사용자에게 물어야 한다(CLAUDE.md). */
    suspend fun closeWindow(seat: Seat): Boolean
}
