package com.example.carproject.data.fake

import android.util.Log
import kotlinx.coroutines.delay

/** TTS를 못 쓸 때. 읽는 척 시간만 보낸다. */
class FakeVoiceRepository {
    suspend fun speak(text: String): Boolean {
        Log.i("FakeTts", "읽기: $text")
        delay(text.length * 90L)
        return true
    }
}
