package io.github.dragjon.eigentone.data

import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.UUID
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.Inflater
import kotlinx.serialization.json.Json

/** Turns a preset into a short text code that can be sent to someone and pasted back in. */
object ShareCode {
    private const val PREFIX = "eigentone1:"
    private const val MAX_CODE_CHARS = 64 * 1024
    private const val MAX_JSON_BYTES = 256 * 1024

    private val json = Json { ignoreUnknownKeys = true }

    fun encode(preset: Preset): String {
        val text = json.encodeToString(Preset.serializer(), preset.copy(tone = preset.tone.copy(trimDb = 0.0)))
        val bytes = ByteArrayOutputStream()
        DeflaterOutputStream(bytes, Deflater(Deflater.BEST_COMPRESSION)).use { it.write(text.encodeToByteArray()) }
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray())
    }

    /** Returns null for anything that isn't a valid code. Codes are checked, since anyone could edit one. */
    fun decode(code: String): Preset? {
        val text = code.trim()
        val body = text.substringAfter(PREFIX, "").filterNot(Char::isWhitespace)
        if (body.isEmpty() || body.length > MAX_CODE_CHARS) return null
        return try {
            val text = inflate(Base64.getUrlDecoder().decode(body)) ?: return null
            val preset = json.decodeFromString(Preset.serializer(), text)
            if (preset.name.isBlank()) return null
            BackupCodec.sanitise(preset).copy(id = "user:${UUID.randomUUID()}")
        } catch (e: Exception) {
            null
        }
    }

    /** Stops at a size limit, so a code can't expand into something enormous. */
    private fun inflate(input: ByteArray): String? {
        val inflater = Inflater()
        try {
            inflater.setInput(input)
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(8 * 1024)
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                if (count == 0 && (inflater.needsInput() || inflater.needsDictionary())) return null
                out.write(buffer, 0, count)
                if (out.size() > MAX_JSON_BYTES) return null
            }
            return out.toByteArray().decodeToString()
        } finally {
            inflater.end()
        }
    }
}
