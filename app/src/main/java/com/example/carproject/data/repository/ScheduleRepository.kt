package com.example.carproject.data.repository

import com.example.carproject.data.model.CalendarEvent
import com.example.carproject.data.model.Mention

interface CalendarRepository {
    fun todayEvents(): List<CalendarEvent>
}

interface MessageRepository {
    fun unreadMentions(): List<Mention>

    /** 내가 속한 팀 채널 이름. "#" 없이. */
    fun teamChannel(): String

    /** 팀 채널에 메시지를 보낸다. 실제로 보내기 전에 반드시 사용자에게 물어야 한다. */
    suspend fun sendToTeam(text: String): Boolean
}
