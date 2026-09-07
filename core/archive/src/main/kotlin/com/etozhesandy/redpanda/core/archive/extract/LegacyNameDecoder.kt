package com.etozhesandy.redpanda.core.archive.extract

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/**
 * Repairs one zip entry name that was written without the UTF-8 flag, deciding the encoding **per
 * entry** rather than once for the whole archive.
 *
 * The per-entry decision is the point. Real exports are mixed — the Windows zip tools that write
 * them use the OEM codepage by default and set the UTF-8 flag only on the names that codepage
 * cannot represent — and one archive-wide choice mis-decodes whichever half loses the vote. That
 * showed up as an export whose `Диалоги/` was found but whose contact folders were not, so the
 * dialogs imported empty.
 *
 * An unflagged name reaches us as zip4j's CP437 decode. CP437 maps all 256 byte values to distinct
 * characters, so re-encoding recovers the original bytes exactly and they can be decoded again with
 * the codepage that was really used. Three candidates are tried, in the order they occur in the
 * wild:
 *  - `zip` on macOS/Linux writes UTF-8 bytes but never sets the flag, so valid UTF-8 settles it;
 *  - older VK export tools write CP866 (DOS Cyrillic);
 *  - Windows GUI tools occasionally write CP1251 (Windows Cyrillic).
 *
 * CP866 and CP1251 cannot be told apart by validity — every byte decodes in both — so they are
 * scored by how much the result looks like a real file name (see [plausibility]). A CP1251 name
 * read as CP866 comes out full of box-drawing characters, and vice versa, which is exactly what the
 * score punishes.
 */
internal object LegacyNameDecoder {

    /** [cp437Name] is the entry name as zip4j decoded it, i.e. the raw bytes seen through CP437. */
    fun decode(cp437Name: String): String {
        if (cp437Name.none { it.code > 127 }) return cp437Name
        val cp437 = CP437 ?: return cp437Name
        val bytes = runCatching { cp437Name.toByteArray(cp437) }.getOrNull() ?: return cp437Name

        decodeStrictly(bytes, Charsets.UTF_8)?.let { return it }

        val candidates = listOfNotNull(
            CP866?.let { charset -> runCatching { String(bytes, charset) }.getOrNull() },
            CP1251?.let { charset -> runCatching { String(bytes, charset) }.getOrNull() },
        )
        // Ties keep CP866: it is the codepage verified against real exports, and the fallback that
        // was in place before CP1251 was ever considered.
        return candidates.maxByOrNull(::plausibility) ?: cp437Name
    }

    /**
     * How much [name] reads like a file name someone actually typed.
     *
     * Letters and digits earn, the punctuation that shows up in export folder names (`Имя (id1)`,
     * `фото+видео`) is neutral-positive, and the box-drawing and block characters that a
     * wrong Cyrillic codepage produces cost far more than a letter earns.
     */
    private fun plausibility(name: String): Int = name.sumOf { char ->
        when {
            char in '─'..'◿' -> -4
            char.isCyrillicLetter() -> 3
            char.isLetterOrDigit() -> 2
            char in NEUTRAL_CHARS -> 1
            else -> -1
        }
    }

    private fun Char.isCyrillicLetter(): Boolean = this in 'Ѐ'..'ӿ' && isLetter()

    /** Null when [bytes] are not valid [charset], so an invalid decode never wins by default. */
    private fun decodeStrictly(bytes: ByteArray, charset: Charset): String? = runCatching {
        charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    }.getOrNull()

    private const val NEUTRAL_CHARS = " ()[]{}_-.,!+@#&~'`$%=;"

    /**
     * Single-byte, so decoding to it and back recovers the raw entry-name bytes unchanged. None of
     * these is part of the charset set every Android build is required to ship, so all are resolved
     * defensively and decoding just backs off when they're missing.
     */
    private val CP437: Charset? = runCatching { Charset.forName("Cp437") }.getOrNull()

    private val CP866: Charset? = runCatching { Charset.forName("CP866") }.getOrNull()

    private val CP1251: Charset? = runCatching { Charset.forName("windows-1251") }.getOrNull()
}
