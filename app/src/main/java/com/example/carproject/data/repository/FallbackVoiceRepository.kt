package com.example.carproject.data.repository

import com.example.carproject.data.fake.FakeVoiceRepository
import com.example.carproject.data.pleos.PleosVoiceRepository

/** Pleos TTS를 먼저 쓰고, 지원하지 않으면 가짜로 대신한다. */
class FallbackVoiceRepository(
    private val pleos: PleosVoiceRepository,
    private val fake: FakeVoiceRepository,
) : VoiceRepository {
    override suspend fun speak(text: String): Boolean =
        if (pleos.ensureReady()) pleos.speak(text) else fake.speak(text)

    override fun stop() = pleos.stop()
}
