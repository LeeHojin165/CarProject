package com.example.carproject.data.model

/** 기어. P면 주차, 나머지는 주행 중으로 본다. */
enum class Gear { P, R, N, D;
    val isParked: Boolean get() = this == P
}

enum class Seat(val label: String) {
    Driver("운전석"), Passenger("조수석"), RearLeft("뒷좌석 왼쪽"), RearRight("뒷좌석 오른쪽")
}

/** 타이어 공기압(psi). */
data class TirePressures(val frontLeft: Int, val frontRight: Int, val rearLeft: Int, val rearRight: Int) {
    val all get() = listOf(frontLeft, frontRight, rearLeft, rearRight)
    fun isNormal(min: Int = 30, max: Int = 40) = all.all { it in min..max }
}

/**
 * 차량 상태 한 장면. Pleos Vehicle SDK 값이 들어오고, 지원하지 않는 항목은 가짜 값으로 채운다.
 * evBatteryPercent가 있으면 전기차, 없으면 fuelPercent를 쓴다.
 */
data class VehicleStatus(
    val gear: Gear = Gear.P,
    val fuelPercent: Int? = null,
    val evBatteryPercent: Int? = 62,
    val rangeKm: Int = 280,
    val tires: TirePressures = TirePressures(35, 35, 34, 35),
    val brakeWarning: Boolean = false,
    val openDoors: Set<Seat> = emptySet(),
    val openWindows: Set<Seat> = emptySet(),
    val lightsOn: Boolean = false,
    val warningLights: List<String> = emptyList(),
    val washerFluidLow: Boolean = true,
)
