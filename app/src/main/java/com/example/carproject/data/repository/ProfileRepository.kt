package com.example.carproject.data.repository

import com.example.carproject.data.model.Profile
import com.example.carproject.data.model.TripLogEntry

interface ProfileRepository {
    fun profile(): Profile
}

interface TripLogRepository {
    /** 지역 평균 유가(원/L). */
    fun fuelPricePerLiter(): Int
    fun entries(): List<TripLogEntry>
    suspend fun save(entry: TripLogEntry)
}
