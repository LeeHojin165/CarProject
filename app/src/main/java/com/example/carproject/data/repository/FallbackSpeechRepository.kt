package com.example.carproject.data.repository

import android.util.Log
import com.example.carproject.data.fake.FakeSpeechRepository
import com.example.carproject.data.model.SpeechEvent
import com.example.carproject.data.pleos.PleosSpeechRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/** Pleos STT를 먼저 쓰고, 지원하지 않으면 가짜로 대신한다. */
class FallbackSpeechRepository(
    private val pleos: PleosSpeechRepository,
    private val fake: FakeSpeechRepository,
) : SpeechRepository {
    /**
     * 시연 녹화용: 다음 한 번의 듣기는 마이크 대신 이 문장을 말한 것처럼 흘려보낸다.
     * 에뮬레이터에 사람 목소리를 넣을 수 없을 때 adb(--es say "...")로 넣는다.
     */
    @Volatile var scriptedNext: String? = null

    /** 시연 녹화용: 켜면 마이크를 쓰지 않고, [scriptedNext]가 들어올 때까지 듣는 중으로 기다린다. */
    @Volatile var scriptOnly = false

    override fun listen(): Flow<SpeechEvent> = flow {
        if (scriptOnly && scriptedNext == null) {
            emit(SpeechEvent.Listening)
            while (scriptedNext == null) delay(100)
        }
        val scripted = scriptedNext
        if (scripted != null) {
            scriptedNext = null
            Log.i("Speech", "시연용 음성 입력: $scripted")
            emitAll(FakeSpeechRepository(scripted).listen())
        } else if (pleos.ensureReady()) {
            emitAll(pleos.listen())
        } else {
            Log.i("Speech", "Pleos STT 미지원 → 가짜 음성 인식 사용")
            emitAll(fake.listen())
        }
    }
}
