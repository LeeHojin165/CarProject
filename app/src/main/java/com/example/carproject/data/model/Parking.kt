package com.example.carproject.data.model

import java.time.LocalTime

/** 주차한 곳. floor는 "B3"·"지상", spot은 기둥 번호 "C12". 알아듣지 못한 말은 note에 그대로 둔다. */
data class ParkingSpot(
    val placeName: String,
    val floor: String?,
    val spot: String?,
    val parkedAt: LocalTime,
    val note: String? = null,
) {
    /** "B3 · 기둥 C12" */
    val label: String
        get() = listOfNotNull(floor, spot?.let { "기둥 $it" }).joinToString(" · ").ifBlank { note ?: "위치 메모 없음" }
}
