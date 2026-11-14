package com.example.carproject.data.fake

import android.util.Log
import com.example.carproject.data.model.CommuteSettings
import com.example.carproject.data.model.CorporateCarInfo
import com.example.carproject.data.model.LinkedAccount
import com.example.carproject.data.model.MonthlyRecord
import com.example.carproject.data.model.PickupHistory
import com.example.carproject.data.model.ProactiveSettings
import com.example.carproject.data.model.Profile
import com.example.carproject.data.model.TripLogEntry
import com.example.carproject.data.repository.ProfileRepository
import com.example.carproject.data.repository.RecordRepository
import com.example.carproject.data.repository.SettingsRepository
import com.example.carproject.data.repository.TripLogRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.LocalTime

class FakeProfileRepository : ProfileRepository {
    override fun profile() = Profile(name = "[이름]", department = "[부서]", initial = "이")

    override fun linkedAccounts() = listOf(
        LinkedAccount("Google", "G", "[gmail 주소]", connected = true),
        LinkedAccount("Slack", "S", "[워크스페이스 이름]", connected = true),
    )

    override suspend fun disconnect() {
        Log.i("FakeAuth", "이 차에서 연결 끊기")
        delay(300)
    }
}

class FakeSettingsRepository : SettingsRepository {
    private val state = MutableStateFlow(ProactiveSettings())
    override val proactive: StateFlow<ProactiveSettings> = state.asStateFlow()
    override fun update(transform: (ProactiveSettings) -> ProactiveSettings) = state.update(transform)

    override fun commute() = CommuteSettings(
        workName = "[회사 이름]", workAddress = "[회사 주소]",
        arriveBy = LocalTime.of(9, 0), workDays = "월요일부터 금요일",
        homeAddress = "[집 주소]",
        lateChannel = "[팀 채널]", lateExtraRecipients = 1,
        lateThresholdMinutes = 5, lateMessage = "[n]분 늦어요",
    )
}

/** 21·25의 이번 달 숫자. 실제 기록이 쌓이기 전이라 디자인 값을 쓰고, 오늘 한 일만 더한다. */
class FakeRecordRepository : RecordRepository {
    override fun thisMonth() = MonthlyRecord(
        month = LocalDate.now().monthValue, trips = 38, totalKm = 812,
        commuteToWork = 19, commuteToHome = 19,
        avgCommuteMinutes = 34, avgCommuteDeltaMinutes = -3,
        onTime = 16, lateNoticesSent = 3,
        savedFoodKg = 4.8, savedWon = 32_400,
    )

    override fun recentPickups() = listOf(
        PickupHistory(LocalDate.now().minusDays(1), "[빵집 이름]", "마감 빵 묶음 · 픽업 1234", 1.2),
        PickupHistory(LocalDate.now().minusDays(3), "[반찬가게 이름]", "반찬 3종 세트", 0.9),
        PickupHistory(LocalDate.now().minusDays(8), "[샐러드 가게 이름]", "샐러드 2팩", 0.6),
    )

    override fun corporateCar() = CorporateCarInfo(
        companyName = "[회사 이름]", businessNumber = "[000-00-00000]",
        carModel = "[차종]", plateNumber = "[00가 0000]", fuelPriceSource = "지역 평균 유가",
        monthCommuteKm = 520, monthBusinessKm = 292, monthTotalKm = 870,
    )
}

class FakeTripLogRepository : TripLogRepository {
    private val saved = mutableListOf(
        TripLogEntry(LocalDate.now().minusDays(1), "집 → 회사", true, "[부서] · [이름]", 32_376, 32_397, "출퇴근", 3_000),
        TripLogEntry(LocalDate.now().minusDays(2), "회사 → 집", true, "[부서] · [이름]", 32_353, 32_376, "출퇴근", 3_300),
        TripLogEntry(LocalDate.now().minusDays(4), "회사 → [협력사 B]", false, "[부서] · [이름]", 32_312, 32_353, "협력사 회의", 5_800),
    )

    override fun fuelPricePerLiter() = 1_700
    override fun entries(): List<TripLogEntry> = saved.toList()

    override suspend fun save(entry: TripLogEntry) {
        Log.i("FakeTripLog", "운행기록 저장: ${entry.date} ${entry.distanceKm}km ${entry.purpose} ${entry.fuelCostWon}원")
        saved.add(0, entry)
    }

    override suspend fun exportExcel(): Boolean {
        Log.i("FakeTripLog", "엑셀로 만들어 폰으로 보냄: ${saved.size}건")
        delay(400)
        return true
    }

    override suspend fun sendToSlack(channel: String): Boolean {
        Log.i("FakeSlack", "#$channel 에 운행기록 ${saved.size}건 보냄")
        delay(400)
        return true
    }
}
