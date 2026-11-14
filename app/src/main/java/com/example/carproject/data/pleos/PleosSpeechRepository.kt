package com.example.carproject.data.pleos

import android.content.Context
import android.util.Log
import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.ErrorType
import ai.pleos.playground.stt.listener.OnInitializeListener
import ai.pleos.playground.stt.listener.ResultListener
import com.example.carproject.data.model.SpeechEvent
import com.example.carproject.data.repository.SpeechRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Pleos Connect SpeechToText SDK 연동.
 * STT에는 checkXXXCapabilities가 없어서, CAAS 서비스 initialize 성공 여부로 지원 여부를 판단한다.
 */
class PleosSpeechRepository(context: Context) : SpeechRepository {
    private val appContext = context.applicationContext
    private val initLock = Mutex()
    private var stt: SpeechToText? = null
    private var unavailable = false

    /** 처음 한 번만 초기화한다. 실패하면 이후로도 false. */
    suspend fun ensureReady(): Boolean = initLock.withLock {
        if (stt != null) return true
        if (unavailable) return false
        val ready = withContext(Dispatchers.Main) {
            runCatching {
                val instance = SpeechToText(appContext)
                val ok = withTimeoutOrNull(INIT_TIMEOUT_MS) {
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
                if (ok) {
                    instance.setLocale(Locale.KOREA)
                    stt = instance
                } else {
                    runCatching { instance.release() }
                }
                ok
            }.getOrElse {
                Log.w(TAG, "SpeechToText를 쓸 수 없음", it)
                false
            }
        }
        if (!ready) unavailable = true
        Log.i(TAG, "Pleos STT 사용 가능: $ready")
        ready
    }

    override fun listen(): Flow<SpeechEvent> = callbackFlow {
        val engine = stt
        if (engine == null) {
            trySend(SpeechEvent.Failed("음성 인식을 쓸 수 없어요"))
            close()
            awaitClose {}
            return@callbackFlow
        }
        var lastText = ""
        val listener = object : ResultListener {
            override fun onReady() { trySend(SpeechEvent.Listening) }
            override fun onStartedRecognition() { trySend(SpeechEvent.Listening) }
            override fun onUpdated(p0: String, p1: Boolean) {
                lastText = p0
                if (p1) {
                    trySend(SpeechEvent.Final(p0))
                    close()
                } else {
                    trySend(SpeechEvent.Partial(p0))
                }
            }
            override fun onEndedRecognition() {
                // 확정 결과 없이 끝나면 마지막 중간 결과를 확정으로 본다.
                if (lastText.isNotBlank()) trySend(SpeechEvent.Final(lastText))
                else trySend(SpeechEvent.Failed("잘 못 들었어요"))
                close()
            }
            override fun onError(p0: ErrorType) {
                Log.w(TAG, "인식 오류: $p0")
                trySend(SpeechEvent.Failed(p0.toMessage()))
                close()
            }
            override fun onUpdatedEpdData(p0: Long, p1: Long) = Unit
            override fun onUpdatedRms(p0: Double) = Unit
        }
        engine.addListener(listener)
        engine.request()
        // 말이 없으면 끝없이 듣지 않게 멈춘다.
        launch {
            delay(SILENCE_TIMEOUT_MS)
            if (lastText.isBlank()) {
                trySend(SpeechEvent.Failed("말씀이 없어서 멈췄어요"))
                close()
                return@launch
            }
            delay(MAX_LISTEN_MS - SILENCE_TIMEOUT_MS)
            trySend(SpeechEvent.Final(lastText))
            close()
        }
        awaitClose {
            runCatching { engine.stop() }
            runCatching { engine.removeListener(listener) }
        }
    }.flowOn(Dispatchers.Main)

    private fun ErrorType.toMessage() = when (this) {
        ErrorType.TIMEOUT -> "말씀이 없어서 멈췄어요"
        ErrorType.NETWORK_ERROR -> "인터넷 연결을 확인해 주세요"
        ErrorType.MIC_ERROR, ErrorType.AUDIO_ERROR -> "마이크를 쓸 수 없어요"
        ErrorType.PERMISSION_DENIED, ErrorType.UNAUTHENTICATED -> "음성 인식 권한이 없어요"
        else -> "잘 못 들었어요"
    }

    private companion object {
        const val TAG = "PleosStt"
        const val INIT_TIMEOUT_MS = 5_000L
        const val SILENCE_TIMEOUT_MS = 8_000L
        const val MAX_LISTEN_MS = 15_000L
    }
}
