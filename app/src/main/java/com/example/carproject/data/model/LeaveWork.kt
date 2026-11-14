package com.example.carproject.data.model

import java.time.LocalTime

/** 퇴근길 경로 위 충전소. */
data class ChargingStation(
    val id: String,
    val name: String,
    val place: Place,
    /** "급속 100kW · 25분이면 80%" */
    val description: String,
    /** 지도 위 표시 "급속 · 2대 비어 있음" */
    val mapLabel: String,
    /** 경로에서 돌아가는 시간(분) */
    val detourMinutes: Int,
)

/** 마감 할인 가게. */
data class PickupStore(
    val id: String,
    val name: String,
    val place: Place,
    val category: String,
    /** 상자에 담기는 것. "빵", "유제품", "반찬" */
    val itemNoun: String,
    val detailCategory: String,
    val pickupStart: LocalTime,
    val pickupEnd: LocalTime,
    val detourMinutes: Int,
    val remaining: Int,
    val discountPercent: Int,
    val price: Int,
    val originalPrice: Int,
    val tags: List<String>,
    val storage: String,
    val eatBy: String,
    val minutesToHomeAfter: Int,
    /** 가게가 알려준 주차 정보 */
    val parking: String,
    /** 도로 어느 쪽에 있는지 */
    val side: String,
)

data class Reservation(val store: PickupStore, val pickupNumber: String, val payUntil: LocalTime)

/** 11 퇴근 모드에서 바뀌는 것 하나. */
data class OffModeChange(val title: String, val description: String, val value: String)

/** 내일 첫 일정과 옮길 할 일. */
data class TomorrowPreview(val firstEventTitle: String, val firstEventTime: LocalTime, val pendingTasks: Int)
