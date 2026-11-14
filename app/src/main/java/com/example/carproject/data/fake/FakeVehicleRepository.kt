package com.example.carproject.data.fake

import com.example.carproject.data.model.Seat
import com.example.carproject.data.model.VehicleStatus
import android.util.Log
import kotlinx.coroutines.delay
import com.example.carproject.data.repository.VehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Pleos SDK 연동 전·미지원 시 쓰는 가짜 차량 상태.
 * 시연용 디버그 메뉴가 [edit]로 값을 바꿔 다음 장면을 띄운다.
 */
class FakeVehicleRepository(initial: VehicleStatus = VehicleStatus()) : VehicleRepository {
    private val state = MutableStateFlow(initial)
    override val status: StateFlow<VehicleStatus> = state.asStateFlow()

    fun edit(transform: (VehicleStatus) -> VehicleStatus) = state.update(transform)

    override suspend fun keepClimate(tempC: Int, seatHeatLevel: Int): Boolean {
        Log.i("FakeVehicle", "공조 유지: ${tempC}도, 열선 ${seatHeatLevel}단")
        state.update { it.copy(keepClimateTempC = tempC) }
        return true
    }

    override suspend fun ventilate(seconds: Int): Boolean {
        Log.i("FakeVehicle", "환기: 앞좌석 창문 조금 열기, ${seconds}초 뒤 닫기")
        return true
    }

    override suspend fun closeWindow(seat: Seat): Boolean {
        delay(800) // 창문이 올라가는 시간
        state.update { it.copy(openWindows = it.openWindows - seat) }
        return true
    }
}
