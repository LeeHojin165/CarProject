package com.example.carproject.data.fake

import android.util.Log
import com.example.carproject.data.model.Profile
import com.example.carproject.data.model.TripLogEntry
import com.example.carproject.data.repository.ProfileRepository
import com.example.carproject.data.repository.TripLogRepository
import java.time.LocalDate

class FakeProfileRepository : ProfileRepository {
    override fun profile() = Profile(name = "[이름]", department = "[부서]", initial = "이")
}

class FakeTripLogRepository : TripLogRepository {
    private val saved = mutableListOf(
        TripLogEntry(LocalDate.now().minusDays(4), "[부서] · [이름]", 32_330, 32_371, "협력사 회의", 5_800),
        TripLogEntry(LocalDate.now().minusDays(11), "[부서] · [이름]", 32_190, 32_252, "지점 점검", 8_800),
    )

    override fun fuelPricePerLiter() = 1_700
    override fun entries(): List<TripLogEntry> = saved.toList()

    override suspend fun save(entry: TripLogEntry) {
        Log.i("FakeTripLog", "운행기록 저장: ${entry.date} ${entry.distanceKm}km ${entry.purpose} ${entry.fuelCostWon}원")
        saved.add(0, entry)
    }
}
