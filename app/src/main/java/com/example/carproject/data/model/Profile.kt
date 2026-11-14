package com.example.carproject.data.model

import java.time.LocalDate

/** 이 차 프로필에 로그인한 사용자. */
data class Profile(val name: String, val department: String, val initial: String)

/** 업무용승용차 운행기록 한 건(국세청 별지 서식 항목 기준). */
data class TripLogEntry(
    val date: LocalDate,
    val user: String,
    val startOdometerKm: Int,
    val endOdometerKm: Int,
    val purpose: String,
    val fuelCostWon: Int,
) {
    val distanceKm: Int get() = endOdometerKm - startOdometerKm
}
