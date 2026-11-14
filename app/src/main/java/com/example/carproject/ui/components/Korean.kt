package com.example.carproject.ui.components

/** 마지막 한글 글자에 받침이 있는지. 받침이 ㄹ이면 "로"를 쓰므로 따로 본다. */
private fun lastHangulJong(word: String): Int? {
    val ch = word.lastOrNull { it in '가'..'힣' } ?: return null
    return (ch - '가') % 28
}

/** "빵이", "반찬이", "유제품이", "커피가"처럼 이/가를 붙인다. */
fun withIGa(word: String): String {
    val jong = lastHangulJong(word)
    return if (jong == null || jong == 0) "${word}가" else "${word}이"
}

/** "회사로", "집으로", "서울로"처럼 (으)로를 붙인다. */
fun withRo(word: String): String {
    val jong = lastHangulJong(word)
    return if (jong == null || jong == 0 || jong == 8) "${word}로" else "${word}으로"
}
