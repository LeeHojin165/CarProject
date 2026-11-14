package com.example.carproject.data.model

import java.time.LocalTime

data class CalendarEvent(
    val time: LocalTime,
    val title: String,
    val location: String? = null,
    /** 회사 밖에서 하는 일정이라 출장으로 이어질 수 있다. */
    val outside: Boolean = false,
)

/** 나를 언급한 Slack 메시지. */
data class Mention(val sender: String, val channel: String, val text: String)
