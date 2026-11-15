package com.example.carproject.debug

import android.util.Log
import com.example.carproject.data.AppClock
import com.example.carproject.data.fake.FakeCalendarRepository
import com.example.carproject.data.fake.FakeMessageRepository
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
    private val calendar: FakeCalendarRepository,
    private val messages: FakeMessageRepository,
    private val speech: com.example.carproject.data.repository.FallbackSpeechRepository,
    private val auth: com.example.carproject.data.fake.FakeAuthRepository,
) {
    /** 휴대폰으로 QR을 찍은 것처럼 로그인 단계를 진행한다(시연 컷 0). */
    fun phoneLogin() {
        Log.i(TAG, "휴대폰 QR 로그인")
        auth.phoneScanned = true
    }

    /** 다음 음성 입력을 이 문장으로 대신한다(녹화용). */
    fun setScriptOnlyMic(on: Boolean) {
        Log.i(TAG, "마이크 대신 시연 문장만: $on")
        speech.scriptOnly = on
    }

    fun sayNext(text: String) {
        Log.i(TAG, "다음 음성 입력: $text")
        speech.scriptedNext = text
    }

    val gear: StateFlow<Gear> get() = gears.gear
    val vehicleStatus: StateFlow<VehicleStatus> get() = vehicle.status

    val mode: StateFlow<CommuteMode> get() = driving.mode

    fun setGear(gear: Gear) = gears.overrideForDemo(gear)

    /** 출근(라이트)·퇴근(다크) 장면 전환. 퇴근 시작 화면이 생기기 전까지 10번 장면을 이걸로 띄운다. */
    fun setMode(mode: CommuteMode) {
        driving.setMode(mode)
        // 같은 모드를 다시 눌러도 시계를 08:12 / 18:00으로 되돌린다(컷마다 같은 시각에서 시작).
        AppClock.setTo(if (mode == CommuteMode.Evening) AppClock.EVENING else AppClock.MORNING)
    }

    fun setBatteryLow(low: Boolean) = edit("배터리 ${if (low) "부족" else "보통"}") {
        // 부족 값은 12 충전 제안 디자인과 맞춘다(14% · 58km).
        if (low) it.copy(evBatteryPercent = 14, rangeKm = 58) else it.copy(evBatteryPercent = 62, rangeKm = 280)
    }

    fun setPassengerWindowOpen(open: Boolean) = edit("조수석 창문 ${if (open) "열림" else "닫힘"}") {
        it.copy(openWindows = if (open) it.openWindows + Seat.Passenger else it.openWindows - Seat.Passenger)
    }

    fun setWasherLow(low: Boolean) = edit("워셔액 ${if (low) "부족" else "충분"}") { it.copy(washerFluidLow = low) }

    fun setOutsideCold(cold: Boolean) = edit("바깥 기온 ${if (cold) 3 else 12}도") {
        it.copy(outsideTempC = if (cold) 3 else 12)
    }

    /** 오늘 일정에 11:30 고객사 미팅을 넣는다(시연 컷 5). 출장 묻기 화면을 열어 바로 제안이 보이게 한다. */
    fun addClientMeeting() {
        Log.i(TAG, "오늘 일정 넣기: 11:30 고객사 미팅")
        calendar.clientMeetingAdded = true
    }

    /** Slack 멘션 1개(시연 컷 3). 다음에 여는 브리핑부터 보인다. */
    fun addMention() {
        Log.i(TAG, "Slack 멘션 1개")
        messages.mentionCount = 1
    }

    /** 처음 실행 상태로 되돌리고 로그인(QR) 화면을 연다(시연 컷 0). */
    fun resetFirstRun() {
        Log.i(TAG, "처음 실행으로 초기화")
        auth.phoneScanned = false
        calendar.clientMeetingAdded = false
        messages.mentionCount = 0
        gears.overrideForDemo(Gear.P)
        vehicle.edit { VehicleStatus() }
        driving.stopDriving()
        driving.setMode(CommuteMode.Morning)
        router.open(ScreenRequest.Login)
    }

    fun showLateNotice() = driving.showLateNotice()

    fun closePopups() = driving.onDismiss()

    /** 16 픽업 가게 앞 장면으로 바로 간다. */
    fun arriveAtPickup() {
        val store = pickup.storesOnRouteHome().first()
        driving.forceArriveAtPickup(Reservation(store, "1234", store.pickupEnd))
        router.open(ScreenRequest.PickupWait)
    }

    /** 15 픽업 접근 알림. 예약이 없으면 첫 가게로 가짜 예약을 만들어 띄운다. */
    fun showPickupApproach(meters: Int = 300) {
        val store = pickup.storesOnRouteHome().first()
        driving.showPickupApproach(Reservation(store, "1234", store.pickupEnd), meters)
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
