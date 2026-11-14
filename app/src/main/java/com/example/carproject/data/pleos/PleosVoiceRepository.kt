package com.example.carproject.data.pleos

import android.content.Context
import android.util.Log
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Pleos Connect TextToSpeech SDK 연동.
 * STT와 같이 checkXXXCapabilities가 없어서 initialize 성공 여부로 지원 여부를 판단한다.
 */
class PleosVoiceRepository(context: Context) {
    private val appContext = context.applicationContext
    private val initLock = Mutex()
    private var tts: TextToSpeech? = null
    private var unavailable = false

    suspend fun ensureReady(): Boolean = initLock.withLock {
        if (tts != null) return true
        if (unavailable) return false
        val ok = withContext(Dispatchers.Main) {
            runCatching {
                val instance = TextToSpeech(appContext)
                val success = withTimeoutOrNull(INIT_TIMEOUT_MS) {
                    suspendCancellableCoroutine { cont ->
                        instance.initialize(object : OnInitializeListener {
                            override fun onSuccess() { if (cont.isActive) cont.resume(true) }
                            override fun onFailure(p0: ErrorType) {
                                Log.w(TAG, "initialize 실패: $p0")
                                if (cont.isActive) cont.resume(false)
                            }
                        })
                    }
                } ?: false
                if (success) {
                    instance.setLocale(Locale.KOREA)
                    tts = instance
                } else {
                    runCatching { instance.release() }
                }
                success
            }.getOrElse {
                Log.w(TAG, "TextToSpeech를 쓸 수 없음", it)
                false
            }
        }
        if (!ok) unavailable = true
        Log.i(TAG, "Pleos TTS 사용 가능: $ok")
        ok
    }

    suspend fun speak(text: String): Boolean = withContext(Dispatchers.Main) {
        val engine = tts ?: return@withContext false
        var listener: EventListener? = null
        try {
            withTimeoutOrNull(maxDurationMs(text)) {
                suspendCancellableCoroutine { cont ->
                    // 콜백 안에서 리스너를 지우지 않는다. SDK가 목록을 도는 중일 수 있어서 끝난 뒤 finally에서 지운다.
                    val l = object : EventListener {
                        override fun onStart() = Unit
                        override fun onReady() = Unit
                        override fun onUpdatedRms(p0: Double) = Unit
                        override fun onDone() { if (cont.isActive) cont.resume(true) }
                        override fun onStop() { if (cont.isActive) cont.resume(false) }
                        override fun onError(p0: ErrorType) {
                            Log.w(TAG, "읽기 오류: $p0")
                            if (cont.isActive) cont.resume(false)
                        }
                    }
                    listener = l
                    engine.addEventListener(l)
                    cont.invokeOnCancellation { runCatching { engine.stop() } }
                    // 두 번째 인자는 isFinal: 스트리밍의 마지막 조각인지. false면 다음 조각을 기다리느라 끝나지 않는다.
                    engine.speak(text, true)
                }
            } ?: run {
                runCatching { engine.stop() }
                false
            }
        } finally {
            listener?.let { runCatching { engine.removeEventListener(it) } }
        }
    }

    fun stop() {
        runCatching { tts?.stop() }
    }

    /** 응답이 끝내 오지 않을 때를 대비한 상한. 한 글자에 0.2초쯤으로 넉넉히 잡는다. */
    private fun maxDurationMs(text: String) = 5_000L + text.length * 200L

    private companion object {
        const val TAG = "PleosTts"
        const val INIT_TIMEOUT_MS = 5_000L
    }
}
