package com.example.carproject.driving

import android.util.Log
import com.example.carproject.data.model.Gear
import com.example.carproject.data.repository.GearRepository
import com.example.carproject.data.repository.PlaceRepository
import com.example.carproject.data.repository.SettingsRepository
import com.example.carproject.ui.theme.CommuteMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 기어가 P가 아닌 상태에서 P로 바뀌면(CLAUDE.md) 주행 중 팝업을 닫고 앱을 앞으로 불러와 주차 위치를 묻는다.
 */
class ParkingWatcher(
    private val scope: CoroutineScope,
    private val gears: GearRepository,
    private val driving: DrivingAssistant,
    private val places: PlaceRepository,
    private val router: ScreenRouter,
    private val settings: SettingsRepository,
) {
    fun start() {
        scope.launch {
            var previous: Gear? = null
            gears.gear.collect { gear ->
                if (previous != null && previous != Gear.P && gear == Gear.P) onParked()
                previous = gear
            }
        }
    }

    private fun onParked() {
        // 퇴근길 예약한 가게 앞이면 주차 위치 대신 히터를 켜 둘지 묻는다(16).
        if (driving.arriveAtPickupIfPending()) {
            val s = settings.proactive.value
            Log.i(TAG, "P로 바뀜 → 픽업 가게 앞 (히터 묻기 ${s.keepClimate}, 환기 묻기 ${s.ventilate})")
            when {
                s.keepClimate -> router.open(ScreenRequest.PickupWait)
                s.ventilate -> router.open(ScreenRequest.PickupDepart)
                else -> driving.markPickedUp() // 둘 다 꺼져 있으면 묻지 않고 내비를 그대로 둔다
            }
            return
        }
        // 출장지에 도착하면 운행기록을 채운다(20).
        if (driving.tripKind == DrivingAssistant.TripKind.Business && driving.businessTrip != null &&
            settings.proactive.value.corporateCar
        ) {
            Log.i(TAG, "P로 바뀜 → 출장지 도착, 운행기록")
            driving.stopDriving()
            router.open(ScreenRequest.TripLog)
            return
        }
        val place = driving.destination
            ?: if (driving.mode.value == CommuteMode.Evening) places.home() else places.workplace()
        Log.i(TAG, "P로 바뀜 → 주차 위치 묻기: ${place.name}")
        driving.stopDriving()
        router.open(ScreenRequest.ParkAsk(place))
    }

    private companion object {
        const val TAG = "ParkingWatcher"
    }
}
