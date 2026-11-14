package com.example.carproject.data.fake

import android.util.Log
import com.example.carproject.data.model.ParkingSpot
import com.example.carproject.data.repository.ParkingRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalTime

class FakeParkingRepository : ParkingRepository {
    private val history = mutableMapOf(
        "[회사 이름]" to ParkingSpot("[회사 이름]", "B3", "C12", LocalTime.of(8, 47)),
    )
    private val _latest = MutableStateFlow<ParkingSpot?>(null)
    override val latest: StateFlow<ParkingSpot?> = _latest.asStateFlow()

    override fun lastAt(placeName: String) = history[placeName]

    override suspend fun save(spot: ParkingSpot) {
        history[spot.placeName] = spot
        _latest.value = spot
    }

    override suspend fun sendToPhone(spot: ParkingSpot): Boolean {
        Log.i("FakePhone", "폰으로 보냄: 내 차는 ${spot.label}")
        delay(300)
        return true
    }
}
