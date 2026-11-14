package com.example.carproject.data.repository

import android.util.Log
import com.example.carproject.data.fake.FakeSpeechRepository
import com.example.carproject.data.model.SpeechEvent
import com.example.carproject.data.pleos.PleosSpeechRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/** Pleos STT를 먼저 쓰고, 지원하지 않으면 가짜로 대신한다. */
class FallbackSpeechRepository(
    private val pleos: PleosSpeechRepository,
    private val fake: FakeSpeechRepository,
) : SpeechRepository {
    override fun listen(): Flow<SpeechEvent> = flow {
        if (pleos.ensureReady()) {
            emitAll(pleos.listen())
        } else {
            Log.i("Speech", "Pleos STT 미지원 → 가짜 음성 인식 사용")
            emitAll(fake.listen())
        }
    }
}
