package com.example.carproject.data.car

import android.car.Car
import android.car.VehicleGear
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.util.Log
import com.example.carproject.data.model.Gear
import com.example.carproject.data.repository.GearRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AAOS CarPropertyManager의 GEAR_SELECTION으로 기어를 읽는다(CLAUDE.md).
 * 차 서비스가 없거나 속성을 지원하지 않으면 P로 두고, 시연용 [overrideForDemo]로만 바뀐다.
 */
class CarGearRepository(context: Context) : GearRepository {
    private val _gear = MutableStateFlow(Gear.P)
    override val gear: StateFlow<Gear> = _gear.asStateFlow()

    private var car: Car? = null

    private val callback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            val raw = value.value as? Int ?: return
            val mapped = raw.toGear()
            Log.i(TAG, "GEAR_SELECTION=$raw → $mapped")
            _gear.value = mapped
        }

        override fun onErrorEvent(propertyId: Int, zone: Int) {
            Log.w(TAG, "기어 읽기 오류: $propertyId")
        }
    }

    init {
        runCatching {
            if (!context.packageManager.hasSystemFeature("android.hardware.type.automotive")) {
                Log.i(TAG, "차량 기기가 아님 → 기어는 시연용으로만 바뀜")
                return@runCatching
            }
            val c = Car.createCar(context.applicationContext)
            val manager = c.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
            // 지원 여부 확인: 속성 설정이 없으면 이 차는 GEAR_SELECTION을 주지 않는다.
            if (manager.getCarPropertyConfig(VehiclePropertyIds.GEAR_SELECTION) == null) {
                Log.i(TAG, "GEAR_SELECTION 미지원 → 시연용으로만 바뀜")
                c.disconnect()
                return@runCatching
            }
            @Suppress("DEPRECATION")
            manager.registerCallback(callback, VehiclePropertyIds.GEAR_SELECTION, CarPropertyManager.SENSOR_RATE_ONCHANGE)
            car = c
            Log.i(TAG, "GEAR_SELECTION 구독 시작")
        }.onFailure { Log.w(TAG, "Car 서비스 연결 실패", it) }
    }

    override fun overrideForDemo(gear: Gear) {
        Log.i(TAG, "시연용 기어 변경 → $gear")
        _gear.value = gear
    }

    private fun Int.toGear() = when (this) {
        VehicleGear.GEAR_PARK -> Gear.P
        VehicleGear.GEAR_REVERSE -> Gear.R
        VehicleGear.GEAR_NEUTRAL -> Gear.N
        else -> Gear.D
    }

    private companion object {
        const val TAG = "CarGear"
    }
}
