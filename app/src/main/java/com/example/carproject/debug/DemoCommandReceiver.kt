package com.example.carproject.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.util.Log
import com.example.carproject.CarApplication
import com.example.carproject.data.model.Gear
import com.example.carproject.ui.theme.CommuteMode

/**
 * 시연·테스트용 adb 명령. 디버그 빌드에서만 동작한다.
 *   adb shell am broadcast -a com.example.carproject.DEMO -p com.example.carproject --es gear D
 *   ... --es gear P / --es popup late / --es popup close / --es battery low / --es window open / --es screen home
 *   ... --es briefing "판교 카페"   (좌표 없는 목적지로 브리핑 열기: 내비 검색 경로 확인용)
 */
class DemoCommandReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) return
        val demo = (context.applicationContext as CarApplication).container.demoControls
        intent.getStringExtra("gear")?.let { value ->
            runCatching { Gear.valueOf(value.uppercase()) }
                .onSuccess(demo::setGear)
                .onFailure { Log.w(TAG, "모르는 기어: $value") }
        }
        when (intent.getStringExtra("popup")) {
            "late" -> demo.showLateNotice()
            "close" -> demo.closePopups()
            "pickup" -> demo.showPickupApproach()
            "arrive" -> demo.arriveAtPickup()
        }
        intent.getStringExtra("battery")?.let { demo.setBatteryLow(it == "low") }
        intent.getStringExtra("window")?.let { demo.setPassengerWindowOpen(it == "open") }
        intent.getStringExtra("washer")?.let { demo.setWasherLow(it == "low") }
        if (intent.getStringExtra("screen") == "home") demo.goHome()
        intent.getStringExtra("briefing")?.let(demo::openBriefing)
        when (intent.getStringExtra("mode")) {
            "morning" -> demo.setMode(CommuteMode.Morning)
            "evening" -> demo.setMode(CommuteMode.Evening)
        }
    }

    private companion object {
        const val TAG = "DemoCommand"
    }
}
