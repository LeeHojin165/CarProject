package com.example.carproject.data

import android.os.SystemClock
import android.util.Log
import java.time.LocalTime

/**
 * 앱이 쓰는 "지금 시각". 시연 영상은 촬영 시각과 상관없이 아침 8시대·저녁 6시대처럼 보여야 해서
 * 출근 모드면 08:12, 퇴근 모드면 18:00에서 시작해 실제 시간만큼 흘러간다.
 * 08:12에 출발하면 회사까지 34분이라 8시 46분 도착이 된다(시연 컷 3).
 */
object AppClock {
    val MORNING: LocalTime = LocalTime.of(8, 12)
    val EVENING: LocalTime = LocalTime.of(18, 0)

    private var base: LocalTime = MORNING
    private var setAtMs: Long = SystemClock.elapsedRealtime()

    fun now(): LocalTime =
        base.plusSeconds((SystemClock.elapsedRealtime() - setAtMs) / 1000).withNano(0)

    fun setTo(time: LocalTime) {
        Log.i("AppClock", "시각: $time")
        base = time
        setAtMs = SystemClock.elapsedRealtime()
    }
}
