package com.example.carproject.data.repository

/** 문장을 소리 내어 읽는다. */
interface VoiceRepository {
    /** 다 읽을 때까지 기다린다. 중간에 멈추거나 실패하면 false. 취소하면 읽기도 멈춘다. */
    suspend fun speak(text: String): Boolean

    /** 읽고 있던 것을 멈춘다. */
    fun stop()
}
