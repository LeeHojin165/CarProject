package com.example.carproject.data.fake

import com.example.carproject.data.model.CalendarEvent
import com.example.carproject.data.model.Mention
import com.example.carproject.data.repository.CalendarRepository
import com.example.carproject.data.repository.MessageRepository
import android.util.Log
import kotlinx.coroutines.delay
import java.time.LocalTime

/** 고객사 미팅은 시연 메뉴 "오늘 일정 넣기"로 들어온다(시연 컷 5). */
class FakeCalendarRepository : CalendarRepository {
    var clientMeetingAdded = false

    override fun todayEvents() = listOfNotNull(
        CalendarEvent(LocalTime.of(9, 0), "주간 회의"),
        if (!clientMeetingAdded) null else CalendarEvent(
            LocalTime.of(11, 30), "고객사 미팅", location = "[미팅 장소]", outside = true,
            place = FakePlaceRepository.CLIENT,
            tasks = listOf("제안서 최종본 공유", "견적 일정 확인", "다음 미팅 날짜 잡기"),
        ),
        CalendarEvent(LocalTime.of(15, 0), "디자인 리뷰"),
    )
}

/** 멘션은 시연 메뉴 "Slack 멘션 1개"로 들어온다(시연 컷 3). */
class FakeMessageRepository : MessageRepository {
    var mentionCount = 0

    override fun unreadMentions() = listOf(
        Mention("[보낸 사람]", "[채널]", "오늘 회의 자료 미리 봐 주실 수 있을까요?"),
    ).take(mentionCount)

    override fun teamChannel() = "[팀 채널]"

    override suspend fun sendToTeam(text: String): Boolean {
        Log.i("FakeSlack", "#${teamChannel()}에 보냄: $text")
        delay(400)
        return true
    }
}
