package com.example.carproject.data.model

/** 음성 인식 한 번 동안 일어나는 일. */
sealed interface SpeechEvent {
    /** 마이크가 열려 듣고 있다. */
    data object Listening : SpeechEvent

    /** 말하는 중간 결과. */
    data class Partial(val text: String) : SpeechEvent

    /** 말이 끝나 확정된 결과. 이 뒤로는 이벤트가 오지 않는다. */
    data class Final(val text: String) : SpeechEvent

    /** 인식 실패. 다시 시도할 수 있다. */
    data class Failed(val reason: String) : SpeechEvent
}
