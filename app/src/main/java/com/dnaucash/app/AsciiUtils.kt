package com.dnaucash.app

object AsciiUtils {
    fun isAscii(value: String): Boolean = value.all { it.code in 32..126 }
}
