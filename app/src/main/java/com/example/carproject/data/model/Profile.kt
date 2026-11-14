package com.example.carproject.data.model

import java.time.LocalDate

/** 이 차 프로필에 로그인한 사용자. */
data class Profile(val name: String, val department: String, val initial: String)

/** 업무용승용차 운행기록 한 건(국세청 별지 서식 항목 기준). */
data class TripLogEntry(
    val date: LocalDate,
    /** "회사 → [고객사]" */
    val route: String,
    /** 출퇴근용이면 true, 일반 업무용이면 false */
    val commute: Boolean,
    val user: String,
    val startOdometerKm: Int,
    val endOdometerKm: Int,
    val purpose: String,
    val fuelCostWon: Int,
) {
    val distanceKm: Int get() = endOdometerKm - startOdometerKm
}

/** 24 "먼저 물어보기" 스위치. 켜 둔 것만 먼저 말을 건다(실행 전에는 항상 묻는다). */
data class ProactiveSettings(
    val lateAlert: Boolean = true,
    val vehicleCheck: Boolean = true,
    val mentionSummary: Boolean = true,
    val pickupOffer: Boolean = true,
    val keepClimate: Boolean = true,
    /** 디자인 기본값이 꺼짐 */
    val ventilate: Boolean = false,
    val arrivalCheck: Boolean = true,
    /** 25 법인 차량: 켜 두면 주행이 끝날 때마다 운행기록부를 채운다. */
    val corporateCar: Boolean = true,
)

/** 23 직장과 집 / 늦을 때 설정. */
data class CommuteSettings(
    val workName: String,
    val workAddress: String,
    val arriveBy: java.time.LocalTime,
    val workDays: String,
    val homeAddress: String,
    val lateChannel: String,
    val lateExtraRecipients: Int,
    val lateThresholdMinutes: Int,
    val lateMessage: String,
)

/** 21 이번 달 기록. */
data class MonthlyRecord(
    val month: Int,
    val trips: Int,
    val totalKm: Int,
    val commuteToWork: Int,
    val commuteToHome: Int,
    val avgCommuteMinutes: Int,
    val avgCommuteDeltaMinutes: Int,
    val onTime: Int,
    val lateNoticesSent: Int,
    val savedFoodKg: Double,
    val savedWon: Int,
)

data class PickupHistory(val date: LocalDate, val store: String, val detail: String, val weightKg: Double)

/** 25 법인 차량 기본정보. */
data class CorporateCarInfo(
    val companyName: String,
    val businessNumber: String,
    val carModel: String,
    val plateNumber: String,
    val fuelPriceSource: String,
    /** 이번 달 출퇴근용·일반 업무용 주행(km), 기록이 없는 앞쪽 날짜 몫까지 포함한 시연용 값 */
    val monthCommuteKm: Int,
    val monthBusinessKm: Int,
    val monthTotalKm: Int,
)

/** 22 연결된 계정. */
data class LinkedAccount(val service: String, val letter: String, val detail: String, val connected: Boolean)
