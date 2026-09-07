package com.etozhesandy.redpanda.core.archive.extract

import com.etozhesandy.redpanda.core.common.files.isInside
import java.io.File
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
     * unflagged names need correcting — which [LegacyNameDecoder] does, one entry at a time.
     */
    fun extract(archive: File, destination: File, policy: EntryPolicy = EntryPolicy.Full) {
        val zipFile = Zip4jFile(archive)
        if (policy == EntryPolicy.Full && !needsNameRepair(zipFile)) {
            zipFile.extractAll(destination.absolutePath)
            return
        }
        val root = destination.canonicalFile
        for (header in zipFile.fileHeaders) {
            val name = decodeEntryName(header)
            val outFile = File(destination, name.replace('\\', '/'))
            if (!outFile.isInside(root)) continue
            when {
                header.isDirectory -> outFile.mkdirs()
                policy.skips(name, header.uncompressedSize) -> {
                    outFile.parentFile?.mkdirs()
                    outFile.createNewFile()
                }
                // Only ever called for a file entry: handed a *directory* header, zip4j extracts
                // that whole subtree and rebuilds each name with `String.replaceFirst`, whose first
                // argument is a regex — so a folder like `фото+видео` or `Имя (id123)` fails
                // to match its own name and the entire subtree is written under the uncorrected
                // CP437 name instead.
                else -> zipFile.extractFile(header, destination.absolutePath, name)
            }
        }
    }

    /** Decodes [header]'s name, honouring its own UTF-8 flag before asking [LegacyNameDecoder]. */
    private fun decodeEntryName(header: FileHeader): String =
        if (header.isFileNameUTF8Encoded) header.fileName else LegacyNameDecoder.decode(header.fileName)

    /**
     * Whether any entry name needs repairing at all.
     *
     * Entries that set the UTF-8 flag are already decoded correctly by zip4j, and an ASCII-only
     * name is the same in every codepage involved. When no entry is left, `extractAll` is both
     * faster and better tested than walking the headers by hand.
     */
    private fun needsNameRepair(zipFile: Zip4jFile): Boolean = runCatching {
        zipFile.fileHeaders.any { header ->
            !header.isFileNameUTF8Encoded && header.fileName.any { it.code > 127 }
        }
    }.getOrDefault(true)
}
