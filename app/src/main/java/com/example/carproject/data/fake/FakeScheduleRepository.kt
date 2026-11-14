package com.example.carproject.data.fake

import com.example.carproject.data.model.CalendarEvent
import com.example.carproject.data.model.Mention
import com.example.carproject.data.repository.CalendarRepository
import com.example.carproject.data.repository.MessageRepository
import android.util.Log
import kotlinx.coroutines.delay
import java.time.LocalTime

class FakeCalendarRepository : CalendarRepository {
    override fun todayEvents() = listOf(
        CalendarEvent(LocalTime.of(9, 0), "주간 회의"),
        CalendarEvent(LocalTime.of(11, 30), "고객사 미팅", location = "[미팅 장소]", outside = true),
        CalendarEvent(LocalTime.of(15, 0), "디자인 리뷰"),
    )
}

class FakeMessageRepository : MessageRepository {
    override fun unreadMentions() = listOf(
        Mention("[보낸 사람]", "[채널]", "오늘 회의 자료 미리 봐 주실 수 있을까요?"),
    )

    override fun teamChannel() = "[팀 채널]"

    override suspend fun sendToTeam(text: String): Boolean {
        Log.i("FakeSlack", "#${teamChannel()}에 보냄: $text")
        delay(400)
        return true
    }
}
