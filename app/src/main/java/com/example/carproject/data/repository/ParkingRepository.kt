package com.example.carproject.data.repository

import com.example.carproject.data.model.ParkingSpot
import kotlinx.coroutines.flow.StateFlow

interface ParkingRepository {
    /** 가장 최근에 저장한 주차 위치. */
    val latest: StateFlow<ParkingSpot?>

    /** 이 장소에서 지난번에 세운 곳. */
    fun lastAt(placeName: String): ParkingSpot?

    suspend fun save(spot: ParkingSpot)

    /** 폰으로 주차 위치를 보낸다. */
    suspend fun sendToPhone(spot: ParkingSpot): Boolean
}
