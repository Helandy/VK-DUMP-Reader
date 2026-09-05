package com.etozhesandy.redpanda.core.archive.extract

import com.etozhesandy.redpanda.core.common.files.isInside
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import net.lingala.zip4j.model.FileHeader
import net.lingala.zip4j.ZipFile as Zip4jFile

/**
 * Unpacks a zip, repairing entry names that were written in a legacy codepage.
 *
 * Kept apart from [ArchiveExtractorImpl] because none of it touches Android: it is the part worth
 * testing directly, and the bugs it exists to prevent are both invisible until you look at the
 * bytes of a real export.
 */
internal object ZipExtraction {

    /**
     * zip4j is deliberately left on its default charset. Setting `ZipFile.setCharset` applies that
     * charset to *every* entry and takes precedence over each entry's own UTF-8 flag
     * (`HeaderUtil.decodeStringWithCharset` checks the charset first and never looks at the flag),
     * which destroys the archives this is meant to fix: real exports are mixed, because the Windows
     * zip tools that write them use the OEM codepage by default and set the UTF-8 flag only on the
     * names that codepage cannot represent. Forcing CP866 on one such export turned all 270 of its
     * dialogs into `╨Ф╨╕╨░╨╗╨╛╨│╨╕/...`, so `Диалоги/` was not found at all and the import silently
     * fell back to media-only. On the default charset zip4j honours the flag per entry, so only the
     * unflagged names need correcting — which is done here, one entry at a time.
     */
    fun extract(archive: File, destination: File) {
        val zipFile = Zip4jFile(archive)
        val legacy = legacyNameCharset(zipFile)
        if (legacy == null) {
            zipFile.extractAll(destination.absolutePath)
            return
        }
        val root = destination.canonicalFile
        for (header in zipFile.fileHeaders) {
            val name = decodeEntryName(header, legacy)
            val outFile = File(destination, name.replace('\\', '/'))
            if (!outFile.isInside(root)) continue
            if (header.isDirectory) {
                outFile.mkdirs()
            } else {
                // Only ever called for a file entry: handed a *directory* header, zip4j extracts
                // that whole subtree and rebuilds each name with `String.replaceFirst`, whose first
                // argument is a regex — so a folder like `инцест+тройнички` or `Имя (id123)` fails
                // to match its own name and the entire subtree is written under the uncorrected
                // CP437 name instead.
                zipFile.extractFile(header, destination.absolutePath, name)
            }
        }
    }

    /**
     * Decodes [header]'s name, honouring its own UTF-8 flag before falling back to [legacy].
     *
     * An unflagged name reaches us as zip4j's CP437 decode. CP437 maps all 256 byte values to
     * distinct characters, so re-encoding recovers the original bytes exactly and they can be
     * decoded again with the codepage that was really used.
     */
    private fun decodeEntryName(header: FileHeader, legacy: Charset): String =
        if (header.isFileNameUTF8Encoded) {
            header.fileName
        } else {
            CP437?.let { String(header.fileName.toByteArray(it), legacy) } ?: header.fileName
        }

    /**
     * The charset the archive's *unflagged* entry names were really written in, or null when there
     * is nothing to repair and zip4j's own decoding can be trusted for the whole archive.
     *
     * Entries that set the UTF-8 flag are skipped: zip4j already decodes those correctly and they
     * say nothing about how the rest were encoded. For the others zip4j falls back to CP437 and
     * mangles every non-ASCII name — "Диалоги" becomes "ä¿á½«ú¿", which then fails the
     * `Name (idNNN)` folder pattern the parsers match on, so those dialogs are dropped. Two kinds
     * of archive land here, and they need opposite treatment:
     *  - `zip` on macOS/Linux writes UTF-8 bytes but never sets the flag,
     *  - older VK export tools write a legacy DOS codepage (CP866, verified against real exports).
     *
     * The CP437 round-trip above makes them distinguishable: valid UTF-8 means the first kind,
     * anything else means the second.
     */
    private fun legacyNameCharset(zipFile: Zip4jFile): Charset? {
        val cp437 = CP437 ?: return null
        var sawUnflaggedNonAscii = false
        var allValidUtf8 = true
        val decodeFailed = runCatching {
            for (header in zipFile.fileHeaders) {
                if (header.isFileNameUTF8Encoded) continue
                val name = header.fileName
                if (name.none { it.code > 127 }) continue
                sawUnflaggedNonAscii = true
                if (!isValidUtf8(name.toByteArray(cp437))) {
                    allValidUtf8 = false
                    break
                }
            }
        }.isFailure
        if (decodeFailed || !sawUnflaggedNonAscii) return null
        return if (allValidUtf8) Charsets.UTF_8 else CP866
    }

    private fun isValidUtf8(bytes: ByteArray): Boolean = runCatching {
        Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
    }.isSuccess

    /**
     * Single-byte, so decoding to it and back recovers the raw entry-name bytes unchanged. Neither
     * this nor CP866 is part of the charset set every Android build is required to ship, so both
     * are resolved defensively and detection just backs off when they're missing.
     */
    private val CP437: Charset? = runCatching { Charset.forName("Cp437") }.getOrNull()

    private val CP866: Charset? = runCatching { Charset.forName("CP866") }.getOrNull()
}
