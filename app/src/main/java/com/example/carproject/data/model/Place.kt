package com.example.carproject.data.model

/** 장소. 좌표가 없으면(말로만 들은 곳) 내비에서 이름으로 검색한다. */
data class Place(
    val name: String,
    val address: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

/** 최근에 간 곳. lastVisitedLabel은 "지난주 목요일"처럼 화면에 그대로 쓰는 문구. */
data class RecentPlace(val place: Place, val lastVisitedLabel: String)
