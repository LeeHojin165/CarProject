package com.example.carproject.data.repository

import com.example.carproject.data.model.ChargingStation
import com.example.carproject.data.model.OffModeChange
import com.example.carproject.data.model.PickupStore
import com.example.carproject.data.model.Reservation
import com.example.carproject.data.model.TomorrowPreview

/** 퇴근 모드(Slack 상태·방해 금지). */
interface OffModeRepository {
    fun changes(): List<OffModeChange>
    fun tomorrow(): TomorrowPreview
    /** 내일 출근 왕복 거리(km). 충전이 필요한지 판단할 때 쓴다. */
    fun tomorrowRoundTripKm(): Int

    /** 오늘 퇴근길(회사 → 집) 거리. */
    fun homeKm(): Int
    /** 실행 전에 반드시 사용자에게 물어야 한다. */
    suspend fun apply(): Boolean
}

interface ChargingRepository {
    fun stationsOnRouteHome(): List<ChargingStation>
}

interface PickupRepository {
    fun storesOnRouteHome(): List<PickupStore>
    fun store(id: String): PickupStore?
    /** 결제·예약. 실행 전에 반드시 사용자에게 물어야 한다. */
    suspend fun reserve(store: PickupStore): Reservation
}
