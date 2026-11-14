package com.example.carproject.data.repository

import com.example.carproject.data.model.Gear
import kotlinx.coroutines.flow.StateFlow

interface GearRepository {
    val gear: StateFlow<Gear>

    /** 시연용: 기어 값을 강제로 바꾼다. 실제 차에서는 무시될 수 있다. */
    fun overrideForDemo(gear: Gear)
}
