package com.example.carproject.data.repository

import com.example.carproject.data.model.SpeechEvent
import kotlinx.coroutines.flow.Flow

interface SpeechRepository {
    /**
     * 한 번 듣는다. Final이나 Failed가 오면 흐름이 끝난다.
     * 수집을 취소하면 듣기도 멈춘다.
     */
    fun listen(): Flow<SpeechEvent>
}
