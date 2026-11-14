package com.example.carproject.data.fake

import com.example.carproject.data.model.SpeechEvent
import com.example.carproject.data.repository.SpeechRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Pleos STT를 못 쓸 때, 사람이 말한 것처럼 정해진 문장을 천천히 흘려보낸다. */
class FakeSpeechRepository(
    private val script: String = "판교 [거래처 이름] 사옥으로 가줘",
) : SpeechRepository {
    override fun listen(): Flow<SpeechEvent> = flow {
        emit(SpeechEvent.Listening)
        delay(1_500)
        val words = script.split(" ")
        for (i in 1 until words.size) {
            emit(SpeechEvent.Partial(words.take(i).joinToString(" ")))
            delay(350)
        }
        emit(SpeechEvent.Final(script))
    }
}
