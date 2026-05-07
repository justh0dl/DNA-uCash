package com.dnaucash.app

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import java.nio.charset.StandardCharsets

object NdefUtils {
    fun createTextMessage(text: String): NdefMessage {
        val languageCode = "en".toByteArray(StandardCharsets.US_ASCII)
        val textBytes = text.toByteArray(StandardCharsets.UTF_8)
        val payload = ByteArray(1 + languageCode.size + textBytes.size)
        payload[0] = languageCode.size.toByte()
        System.arraycopy(languageCode, 0, payload, 1, languageCode.size)
        System.arraycopy(textBytes, 0, payload, 1 + languageCode.size, textBytes.size)
        val record = NdefRecord(
            NdefRecord.TNF_WELL_KNOWN,
            NdefRecord.RTD_TEXT,
            ByteArray(0),
            payload
        )
        return NdefMessage(arrayOf(record))
    }

    fun readText(tag: Tag): String? {
        val ndef = Ndef.get(tag) ?: return null
        ndef.connect()
        return try {
            val message = ndef.cachedNdefMessage ?: ndef.ndefMessage ?: return null
            val record = message.records.firstOrNull() ?: return null
            val payload = record.payload ?: return null
            if (payload.isEmpty()) return null
            val languageLength = payload[0].toInt() and 0x3F
            String(payload, 1 + languageLength, payload.size - 1 - languageLength, StandardCharsets.UTF_8)
        } finally {
            runCatching { ndef.close() }
        }
    }

    fun writeText(tag: Tag, text: String) {
        val message = createTextMessage(text)
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            ndef.connect()
            try {
                require(ndef.isWritable) { "Tag is not writable" }
                if (ndef.maxSize < message.toByteArray().size) {
                    error("Tag too small for public record")
                }
                ndef.writeNdefMessage(message)
                return
            } finally {
                runCatching { ndef.close() }
            }
        }

        val formatable = NdefFormatable.get(tag) ?: error("Tag does not support NDEF")
        formatable.connect()
        try {
            formatable.format(message)
        } finally {
            runCatching { formatable.close() }
        }
    }
}
