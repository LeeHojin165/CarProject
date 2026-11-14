package com.example.carproject.debug

import android.util.Log
import com.example.carproject.data.fake.FakeVehicleRepository
import com.example.carproject.data.model.Gear
import com.example.carproject.data.model.Place
import com.example.carproject.data.model.Reservation
import com.example.carproject.data.repository.PickupRepository
import com.example.carproject.data.model.Seat
import com.example.carproject.data.model.VehicleStatus
import com.example.carproject.data.repository.GearRepository
import com.example.carproject.driving.DrivingAssistant
import com.example.carproject.driving.ScreenRequest
import com.example.carproject.driving.ScreenRouter
import com.example.carproject.ui.theme.CommuteMode
import kotlinx.coroutines.flow.StateFlow

/**
 * 시연 영상용으로 장면을 강제로 넘기는 명령 모음(CLAUDE.md "시연 버튼").
 * 화면의 숨은 디버그 메뉴와 adb 명령(DemoCommandReceiver)이 함께 쓴다.
 */
class DemoControls(
    private val gears: GearRepository,
    private val vehicle: FakeVehicleRepository,
    private val driving: DrivingAssistant,
    private val router: ScreenRouter,
    private val pickup: PickupRepository,
) {
    val gear: StateFlow<Gear> get() = gears.gear
    val vehicleStatus: StateFlow<VehicleStatus> get() = vehicle.status

    val mode: StateFlow<CommuteMode> get() = driving.mode

    fun setGear(gear: Gear) = gears.overrideForDemo(gear)

    /** 출근(라이트)·퇴근(다크) 장면 전환. 퇴근 시작 화면이 생기기 전까지 10번 장면을 이걸로 띄운다. */
    fun setMode(mode: CommuteMode) = driving.setMode(mode)

    fun setBatteryLow(low: Boolean) = edit("배터리 ${if (low) "부족" else "보통"}") {
        // 부족 값은 12 충전 제안 디자인과 맞춘다(14% · 58km).
        if (low) it.copy(evBatteryPercent = 14, rangeKm = 58) else it.copy(evBatteryPercent = 62, rangeKm = 280)
    }

    fun setPassengerWindowOpen(open: Boolean) = edit("조수석 창문 ${if (open) "열림" else "닫힘"}") {
        it.copy(openWindows = if (open) it.openWindows + Seat.Passenger else it.openWindows - Seat.Passenger)
    }

    fun setWasherLow(low: Boolean) = edit("워셔액 ${if (low) "부족" else "충분"}") { it.copy(washerFluidLow = low) }

    fun showLateNotice() = driving.showLateNotice()

    fun closePopups() = driving.onDismiss()

    /** 16 픽업 가게 앞 장면으로 바로 간다. */
    fun arriveAtPickup() {
        val store = pickup.storesOnRouteHome().first()
        driving.forceArriveAtPickup(Reservation(store, "1234", store.pickupEnd))
        router.open(ScreenRequest.PickupWait)
    }

    /** 15 픽업 접근 알림. 예약이 없으면 첫 가게로 가짜 예약을 만들어 띄운다. */
    fun showPickupApproach() {
        val store = pickup.storesOnRouteHome().first()
        driving.showPickupApproach(Reservation(store, "1234", store.pickupEnd))
    }

    fun goHome() {
        driving.stopDriving()
        router.open(ScreenRequest.Home)
    }

    /** 좌표 없는 목적지로 브리핑을 연다. 음성으로 말한 곳을 고른 것과 같다. */
    fun openBriefing(placeName: String) = router.open(ScreenRequest.Briefing(Place(placeName, "")))

    private fun edit(label: String, transform: (VehicleStatus) -> VehicleStatus) {
        Log.i(TAG, "시연용 차량 상태 변경: $label")
        vehicle.edit(transform)
    }

    private companion object {
        const val TAG = "DemoControls"
    }
}
