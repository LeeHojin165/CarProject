package com.example.carproject.data.repository

import com.example.carproject.data.model.Place
import com.example.carproject.data.model.RecentPlace

interface PlaceRepository {
    /** 마이페이지에 저장한 직장. */
    fun workplace(): Place
    fun home(): Place
    fun recentPlaces(): List<RecentPlace>
    /** 지금 출발했을 때 걸리는 시간(분). */
    fun estimatedMinutes(to: Place): Int
}
