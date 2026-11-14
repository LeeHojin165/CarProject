package com.example.carproject.data.repository

import com.example.carproject.data.model.CommuteSettings
import com.example.carproject.data.model.CorporateCarInfo
import com.example.carproject.data.model.LinkedAccount
import com.example.carproject.data.model.MonthlyRecord
import com.example.carproject.data.model.PickupHistory
import com.example.carproject.data.model.ProactiveSettings
import com.example.carproject.data.model.Profile
import kotlinx.coroutines.flow.StateFlow
import com.example.carproject.data.model.TripLogEntry

interface ProfileRepository {
    fun profile(): Profile
    fun linkedAccounts(): List<LinkedAccount>
    /** 이 차에서 연결을 끊는다(로그아웃). 실행 전에 반드시 묻는다. */
    suspend fun disconnect()
}

interface SettingsRepository {
    val proactive: StateFlow<ProactiveSettings>
    fun update(transform: (ProactiveSettings) -> ProactiveSettings)
    fun commute(): CommuteSettings
}

interface RecordRepository {
    fun thisMonth(): MonthlyRecord
    fun recentPickups(): List<PickupHistory>
    fun corporateCar(): CorporateCarInfo
}

interface TripLogRepository {
    /** 지역 평균 유가(원/L). */
    fun fuelPricePerLiter(): Int
    fun entries(): List<TripLogEntry>
    suspend fun save(entry: TripLogEntry)
    /** 엑셀 파일로 만들어 폰으로 보낸다. */
    suspend fun exportExcel(): Boolean
    /** Slack으로 보낸다. 실행 전에 반드시 묻는다. */
    suspend fun sendToSlack(channel: String): Boolean
}
