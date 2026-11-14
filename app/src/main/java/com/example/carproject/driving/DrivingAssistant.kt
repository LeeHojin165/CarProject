package com.example.carproject.driving

import android.util.Log
import com.example.carproject.data.model.CalendarEvent
import com.example.carproject.data.model.ChargingStation
import com.example.carproject.data.model.Place
import com.example.carproject.data.model.Reservation
import com.example.carproject.data.repository.NavigationRepository
import com.example.carproject.data.repository.PlaceRepository
import com.example.carproject.data.repository.RouteResult
import com.example.carproject.data.repository.SettingsRepository
import com.example.carproject.data.model.SpeechEvent
import com.example.carproject.data.repository.CalendarRepository
import com.example.carproject.data.repository.MessageRepository
import com.example.carproject.data.repository.SpeechRepository
import com.example.carproject.data.repository.VoiceRepository
import com.example.carproject.ui.theme.CommuteMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime

/** 주행 중 내비 위에 띄우는 작은 팝업. */
sealed interface DrivingPopup {
    data object None : DrivingPopup

    /** design/05_LateNotice.html: 늦을 것 같으니 팀에 알릴지 묻는다. */
    data class LateNotice(
        val lateMinutes: Int,
        val channel: String,
        val message: String,
        val arrivalText: String,
    ) : DrivingPopup

    /** design/06_DrivingCalm.html: 보냈다고 짧게 알린다. */
    data class Sent(val arrivalText: String) : DrivingPopup

    /** design/15_PickupApproach.html: 예약한 가게에 다가가면 픽업 번호와 주차 정보를 알린다. */
    data class PickupApproach(val reservation: Reservation, val distanceText: String) : DrivingPopup
}

/**
 * 주행 중 판단을 맡는 앱 전체 상태 관리자. 내비 위 오버레이에는 ViewModel을 붙일 수 없어서
 * Application 수명으로 두고, OverlayPopupHost가 [popup]을 보고 창을 띄운다.
 * 무엇이든 보내기 전에는 항상 묻는다(CLAUDE.md).
 */
class DrivingAssistant(
    private val scope: CoroutineScope,
    private val calendar: CalendarRepository,
    private val messages: MessageRepository,
    private val speech: SpeechRepository,
    private val voice: VoiceRepository,
    private val places: PlaceRepository,
    private val navigation: NavigationRepository,
    private val settings: SettingsRepository,
) {
    private val _popup = MutableStateFlow<DrivingPopup>(DrivingPopup.None)
    val popup: StateFlow<DrivingPopup> = _popup.asStateFlow()

    private val _mode = MutableStateFlow(CommuteMode.Morning)

    /** 출근길(라이트)인지 퇴근길(다크)인지. 주차·도착 화면과 주행 팝업의 테마를 정한다. */
    val mode: StateFlow<CommuteMode> = _mode.asStateFlow()

    fun setMode(mode: CommuteMode) {
        Log.i(TAG, "모드: $mode")
        // 출근↔퇴근이 바뀌면 이전 길의 목적지는 더 이상 맞지 않는다(퇴근인데 "회사 도착"이 뜨지 않게).
        if (_mode.value != mode) destination = null
        _mode.value = mode
    }

    /** 오늘 이번 길의 거리·시간. 실제 운행 기록이 없어서 가짜 값이다. */
    fun tripSummary(): String = if (_mode.value == CommuteMode.Evening) "18.6km · 44분" else "18.4km · 41분"

    /** 퇴근길에 들를 충전소(12에서 고름). */
    var charger: ChargingStation? = null
        private set

    /** 퇴근길 픽업 예약(14에서 예약). */
    var reservation: Reservation? = null
        private set

    /** 지금 길이 출근·퇴근·출장 중 무엇인지. 장면(05 지각, 20 운행기록)을 고른다. */
    enum class TripKind { Commute, LeaveWork, Business }

    var tripKind = TripKind.Commute
        private set

    /** 법인 차량 출장 정보. 출발 계기판 값은 운행기록(20)에 쓴다. */
    data class BusinessTrip(val place: Place, val event: CalendarEvent?, val startOdometerKm: Int)

    var businessTrip: BusinessTrip? = null
        private set

    /** 01 "네, 출근해요" */
    fun beginCommute() {
        setMode(CommuteMode.Morning)
        tripKind = TripKind.Commute
    }

    /** 19 "출발할게요": 운행기록을 시작한다. */
    fun beginBusinessTrip(place: Place, event: CalendarEvent?, startOdometerKm: Int) {
        setMode(CommuteMode.Morning)
        tripKind = TripKind.Business
        businessTrip = BusinessTrip(place, event, startOdometerKm)
    }

    /** 픽업 진행: 가게 앞에 세우면 AtStore, 받아 오면 PickedUp. */
    enum class PickupStage { None, AtStore, PickedUp }

    var pickupStage = PickupStage.None
        private set

    /** 오늘 구한 음식(kg). 마이페이지 기록에 더한다. */
    var savedFoodKgToday = 0.0
        private set

    /** 픽업을 마친 것으로 한다(16·17을 건너뛸 때). */
    fun markPickedUp() {
        pickupStage = PickupStage.PickedUp
        reservation?.let { savedFoodKgToday += it.store.weightKg }
    }

    /** 기어 P: 예약한 가게 앞에 처음 세운 것이면 true(16을 띄운다). */
    fun arriveAtPickupIfPending(): Boolean {
        if (_mode.value != CommuteMode.Evening || reservation == null || pickupStage != PickupStage.None) return false
        pickupStage = PickupStage.AtStore
        // 가게 앞에 섰으니 "가게에 다가가면" 띄우려던 타이머(15)도 끈다.
        driveJob?.cancel()
        onDismiss()
        return true
    }

    /** 시연용: 예약이 없으면 가짜 예약을 만들고 가게 앞에 선 것으로 한다. */
    fun forceArriveAtPickup(fallback: Reservation) {
        setMode(CommuteMode.Evening)
        if (reservation == null) reservation = fallback
        pickupStage = PickupStage.AtStore
        driveJob?.cancel()
        onDismiss()
    }

    /** 17까지 마쳤다. 받은 음식을 기록하고 집으로 남은 안내를 이어간다. */
    suspend fun finishPickup() {
        markPickedUp()
        navigation.showNavigation()
    }

    /** 01 "퇴근이에요": 퇴근 모드로 바꾸고 지난 퇴근 계획을 비운다. */
    fun beginLeaveWork() {
        setMode(CommuteMode.Evening)
        tripKind = TripKind.LeaveWork
        charger = null
        reservation = null
        pickupStage = PickupStage.None
    }

    fun chooseCharger(station: ChargingStation?) {
        charger = station
    }

    fun setReservation(r: Reservation) {
        reservation = r
    }

    /** 집으로 출발. 고른 충전소·픽업 가게를 경유지로 넣는다(최대 2곳). */
    suspend fun departHome(): RouteResult {
        val home = places.home()
        val stops = listOfNotNull(charger?.place, reservation?.store?.place)
        val result = navigation.startRoute(home, stops)
        if (result !is RouteResult.Failed) onRouteStarted(home)
        return result
    }

    /** 지금 가고 있는(또는 마지막으로 간) 목적지. 주차 위치를 물을 때 장소 이름으로 쓴다. */
    var destination: Place? = null
        private set

    private var driveJob: Job? = null
    private var popupJob: Job? = null

    /** 길안내가 시작됐다. 가짜 교통 정보로 잠시 뒤 지각 여부를 판단한다. */
    fun onRouteStarted(place: Place) {
        Log.i(TAG, "주행 시작: ${place.name}")
        destination = place
        driveJob?.cancel()
        driveJob = scope.launch {
            if (_mode.value == CommuteMode.Morning && tripKind == TripKind.Commute && settings.proactive.value.lateAlert) {
                // 출근길: 가짜 교통 정보로 잠시 뒤 지각 여부를 판단한다.
                delay(LATE_CHECK_DELAY_MS)
                showLateNotice(FAKE_DELAY_MINUTES)
            } else if (reservation != null && pickupStage == PickupStage.None) {
                // 퇴근길: 예약한 가게에 가까워지면 알린다(시연용으로 잠시 뒤).
                delay(PICKUP_APPROACH_DELAY_MS)
                showPickupApproach()
            }
        }
    }

    /** 시연용: 지각 알림을 바로 띄운다. */
    fun showLateNotice(lateMinutes: Int = FAKE_DELAY_MINUTES) {
        val next = calendar.todayEvents().sortedBy { it.time }.firstOrNull { it.time.isAfter(LocalTime.now()) }
            ?: calendar.todayEvents().first()
        val arrival = next.time.plusMinutes(lateMinutes.toLong())
        val notice = DrivingPopup.LateNotice(
            lateMinutes = lateMinutes,
            channel = messages.teamChannel(),
            message = "출근길 정체로 ${lateMinutes}분 정도 늦습니다",
            arrivalText = "%d:%02d".format(arrival.hour, arrival.minute),
        )
        _popup.value = notice
        popupJob?.cancel()
        popupJob = scope.launch {
            // 운전 중에는 화면을 못 보니 질문을 읽어 준 뒤, 말로도 답을 받는다. 답이 없으면 보내지 않고 닫는다.
            val listen = launch {
                voice.speak("${notice.lateMinutes}분 늦을 것 같아요. 팀에 알릴까요?")
                listenForAnswer()
            }
            delay(NO_ANSWER_CLOSE_MS)
            listen.cancel()
            if (_popup.value == notice) _popup.value = DrivingPopup.None
        }
    }

    /** 15: 예약한 가게 300m 앞. 예약이 없으면(시연 메뉴) 첫 가게로 가짜 예약을 만든다. */
    fun showPickupApproach(fallback: Reservation? = null) {
        val r = reservation ?: fallback ?: return
        val popup = DrivingPopup.PickupApproach(r, "300m")
        _popup.value = popup
        popupJob?.cancel()
        popupJob = scope.launch {
            val kind = r.store.place.name.replace("[", "").replace("]", "")
            voice.speak("300미터 앞 ${r.store.side}, 예약한 ${kind}이에요. 픽업 번호는 ${r.pickupNumber.toList().joinToString(" ")}이에요.")
            delay(PICKUP_POPUP_SHOW_MS)
            if (_popup.value == popup) _popup.value = DrivingPopup.None
        }
    }

    /** "보내기" */
    fun onSend() {
        val notice = _popup.value as? DrivingPopup.LateNotice ?: return
        popupJob?.cancel()
        voice.stop()
        popupJob = scope.launch {
            val ok = messages.sendToTeam(notice.message)
            if (!ok) {
                _popup.value = DrivingPopup.None
                return@launch
            }
            _popup.value = DrivingPopup.Sent(notice.arrivalText)
            delay(SENT_SHOW_MS)
            _popup.value = DrivingPopup.None
        }
    }

    /** "괜찮아요" 또는 닫기 */
    fun onDismiss() {
        popupJob?.cancel()
        voice.stop()
        _popup.value = DrivingPopup.None
    }

    fun stopDriving() {
        driveJob?.cancel()
        onDismiss()
    }

    /** 팝업이 떠 있는 동안 계속 듣는다. 말이 없어 끊기면 다시 듣는다. */
    private suspend fun listenForAnswer() {
        while (true) {
            speech.listen().collect { event ->
                if (event !is SpeechEvent.Final) return@collect
                val text = event.text.replace(" ", "")
                when {
                    "보내" in text -> onSend()
                    listOf("괜찮", "아니", "됐어", "하지마").any { it in text } -> onDismiss()
                }
            }
            delay(300)
        }
    }

    private companion object {
        const val TAG = "DrivingAssistant"
        const val FAKE_DELAY_MINUTES = 8
        const val LATE_CHECK_DELAY_MS = 6_000L
        const val NO_ANSWER_CLOSE_MS = 20_000L
        const val SENT_SHOW_MS = 4_000L
        const val PICKUP_APPROACH_DELAY_MS = 8_000L
        const val PICKUP_POPUP_SHOW_MS = 20_000L
    }
}
