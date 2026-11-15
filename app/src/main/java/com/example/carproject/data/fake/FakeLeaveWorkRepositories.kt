package com.example.carproject.data.fake

import com.example.carproject.data.AppClock
import android.util.Log
import com.example.carproject.data.model.ChargingStation
import com.example.carproject.data.model.OffModeChange
import com.example.carproject.data.model.PickupStore
import com.example.carproject.data.model.Place
import com.example.carproject.data.model.Reservation
import com.example.carproject.data.model.TomorrowPreview
import com.example.carproject.data.repository.ChargingRepository
import com.example.carproject.data.repository.OffModeRepository
import com.example.carproject.data.repository.PickupRepository
import kotlinx.coroutines.delay
import java.time.LocalTime

class FakeOffModeRepository : OffModeRepository {
    override fun changes() = listOf(
        OffModeChange("Slack 상태", "팀원에게 보이는 상태 메시지", "퇴근했어요"),
        OffModeChange("Slack 알림", "방해 금지로 설정해요", "내일 09:00까지 끔"),
        OffModeChange("나를 언급한 메시지", "집에 도착하면 몇 개 왔는지만 알려줘요", "도착 후 한 번"),
    )

    override fun tomorrow() = TomorrowPreview("주간 회의", LocalTime.of(10, 0), pendingTasks = 2)

    override fun tomorrowRoundTripKm() = 48

    override suspend fun apply(): Boolean {
        Log.i("FakeSlack", "상태: 퇴근했어요, 방해 금지: 내일 09:00까지")
        delay(400)
        return true
    }
}

/** 판교(회사) → 강남(집) 사이 가짜 충전소. 좌표는 시연용. */
class FakeChargingRepository : ChargingRepository {
    override fun stationsOnRouteHome() = listOf(
        ChargingStation("c1", "[충전소 이름]", Place("[충전소 1]", "[충전소 주소]", 37.4210, 127.0980),
            "급속 100kW · 25분이면 80%", "급속 · 2대 비어 있음", 0),
        ChargingStation("c2", "[충전소 이름]", Place("[충전소 2]", "[충전소 주소]", 37.4560, 127.0640),
            "급속 50kW · 40분이면 80%", "급속 · 1대", 2),
        ChargingStation("c3", "[충전소 이름]", Place("[충전소 3]", "[충전소 주소]", 37.4890, 127.0330),
            "완속 · 집 근처, 밤새 충전", "완속 · 4대", 3),
    )
}

/**
 * 가짜 마감 할인 가게. 픽업 시간은 고정 시각이 아니라 지금 시각 기준으로 잡는다.
 * 촬영 시각이 낮이어도 "시간 안에 도착해요"가 자연스럽게 나오게 하려는 것.
 */
class FakePickupRepository : PickupRepository {
    /** 지금이 포함된 30분 단위 시작 시각. 예: 14:47 → 14:30 */
    private fun windowStart(offsetMinutes: Long = 0): LocalTime {
        val now = AppClock.now()
        return now.withMinute(if (now.minute < 30) 0 else 30).withSecond(0).withNano(0).plusMinutes(offsetMinutes)
    }

    private val stores get() = listOf(
        PickupStore(
            "s1", "[가게 이름]", Place("[빵집]", "[가게 주소]", 37.4300, 127.0900),
            "베이커리", "빵", "베이커리 · 빵 · 디저트 · 리뷰 [n]개",
            windowStart(), windowStart(90), 3, 2, 50, 6_900, 13_800,
            listOf("식빵", "크루아상", "쿠키"), "실온", "오늘 안에", 19, 1.2, "매장 앞 가능", "오른쪽",
        ),
        PickupStore(
            "s2", "[가게 이름]", Place("[유제품 가게]", "[가게 주소]", 37.4600, 127.0600),
            "유제품", "유제품", "유제품 · 치즈 · 요거트 · 리뷰 [n]개",
            windowStart(), windowStart(120), 4, 3, 50, 8_000, 16_000,
            listOf("요거트", "치즈", "우유"), "냉장", "내일까지", 14, 0.9, "건물 주차장 30분 무료", "왼쪽",
        ),
        PickupStore(
            "s3", "[가게 이름]", Place("[반찬가게]", "[가게 주소]", 37.4870, 127.0350),
            "반찬", "반찬", "반찬 · 국 · 리뷰 [n]개",
            windowStart(30), windowStart(120), 2, 1, 40, 9_000, 15_000,
            listOf("나물", "볶음", "국"), "냉장", "내일까지", 6, 0.6, "매장 앞 잠깐 정차", "오른쪽",
        ),
    )

    override fun storesOnRouteHome() = stores
    override fun store(id: String) = stores.firstOrNull { it.id == id }

    override suspend fun reserve(store: PickupStore): Reservation {
        Log.i("FakePickup", "예약·결제: ${store.name} ${store.price}원")
        delay(500)
        return Reservation(store, pickupNumber = "1234", payUntil = store.pickupEnd)
    }
}
