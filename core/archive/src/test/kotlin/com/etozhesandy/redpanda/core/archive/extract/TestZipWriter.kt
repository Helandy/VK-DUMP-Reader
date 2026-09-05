package com.etozhesandy.redpanda.core.archive.extract

import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.Charset
import java.util.zip.CRC32

/**
 * Writes a zip one entry at a time with full control over the raw name bytes and the per-entry
 * UTF-8 flag.
 *
 * `java.util.zip.ZipOutputStream` cannot express what these tests need: it applies a single
 * charset to the whole archive and derives the flag from it (bit 11 is set if and only if that
 * charset is UTF-8), so every name in an archive it writes is encoded the same way. Real VK
 * exports are *mixed* — the Windows tools that produce them write most names in the OEM codepage
 * with no flag, and set the UTF-8 flag only on the few names that codepage cannot represent — and
 * that mix is precisely the case the extractor gets wrong when it is wrong.
 *
 * Entries are stored uncompressed, which keeps the writer to the three record types below.
 */
internal class TestZipWriter {

    private class Entry(
        val nameBytes: ByteArray,
        val utf8Flag: Boolean,
        val content: ByteArray,
        val isDirectory: Boolean,
    )

    private val entries = mutableListOf<Entry>()

    /** A name written in [charset] with the UTF-8 flag left clear — how a legacy tool writes it. */
    fun addUnflagged(name: String, charset: Charset, content: String = ""): TestZipWriter =
        add(name.toByteArray(charset), utf8Flag = false, content = content)

    /** A name written as UTF-8 with the flag set — how the same tool writes what CP866 can't hold. */
    fun addUtf8Flagged(name: String, content: String = ""): TestZipWriter =
        add(name.toByteArray(Charsets.UTF_8), utf8Flag = true, content = content)

    /** A name whose bytes are UTF-8 but whose flag is clear — how `zip` on macOS/Linux writes it. */
    fun addUnflaggedUtf8Bytes(name: String, content: String = ""): TestZipWriter =
        add(name.toByteArray(Charsets.UTF_8), utf8Flag = false, content = content)

    private fun add(nameBytes: ByteArray, utf8Flag: Boolean, content: String): TestZipWriter {
        val isDirectory = nameBytes.isNotEmpty() && nameBytes.last() == '/'.code.toByte()
        entries += Entry(
            nameBytes = nameBytes,
            utf8Flag = utf8Flag,
            content = if (isDirectory) ByteArray(0) else content.toByteArray(Charsets.UTF_8),
            isDirectory = isDirectory,
        )
        return this
    }

    fun writeTo(file: File): File {
        val body = ByteArrayOutputStream()
        val central = ByteArrayOutputStream()
        for (entry in entries) {
            val crc = CRC32().apply { update(entry.content) }.value
            val offset = body.size()
            body.writeLocalHeader(entry, crc)
            body.write(entry.content)
            central.writeCentralHeader(entry, crc, offset)
        }
        file.outputStream().use { out ->
            body.writeTo(out)
            central.writeTo(out)
            out.write(endOfCentralDirectory(entries.size, central.size(), body.size()))
        }
        return file
    }

    private fun ByteArrayOutputStream.writeLocalHeader(entry: Entry, crc: Long) {
        writeInt(LOCAL_HEADER_SIGNATURE)
        writeShort(VERSION_NEEDED)
        writeShort(if (entry.utf8Flag) UTF8_NAME_FLAG else 0)
        writeShort(METHOD_STORED)
        writeShort(0) // modification time
        writeShort(0) // modification date
        writeInt(crc.toInt())
        writeInt(entry.content.size)
        writeInt(entry.content.size)
        writeShort(entry.nameBytes.size)
        writeShort(0) // extra field length
        write(entry.nameBytes)
    }

    private fun ByteArrayOutputStream.writeCentralHeader(entry: Entry, crc: Long, offset: Int) {
        writeInt(CENTRAL_HEADER_SIGNATURE)
        writeShort(VERSION_MADE_BY)
        writeShort(VERSION_NEEDED)
        writeShort(if (entry.utf8Flag) UTF8_NAME_FLAG else 0)
        writeShort(METHOD_STORED)
        writeShort(0) // modification time
        writeShort(0) // modification date
        writeInt(crc.toInt())
        writeInt(entry.content.size)
        writeInt(entry.content.size)
        writeShort(entry.nameBytes.size)
        writeShort(0) // extra field length
        writeShort(0) // comment length
        writeShort(0) // disk number start
        writeShort(0) // internal attributes
        writeInt(if (entry.isDirectory) MS_DOS_DIRECTORY_ATTRIBUTE else 0)
        writeInt(offset)
        write(entry.nameBytes)
    }

    private fun endOfCentralDirectory(count: Int, centralSize: Int, centralOffset: Int): ByteArray =
        ByteArrayOutputStream().apply {
            writeInt(END_OF_CENTRAL_DIRECTORY_SIGNATURE)
            writeShort(0) // this disk
            writeShort(0) // disk with the start of the central directory
            writeShort(count)
            writeShort(count)
            writeInt(centralSize)
            writeInt(centralOffset)
            writeShort(0) // comment length
        }.toByteArray()

    private fun ByteArrayOutputStream.writeShort(value: Int) {
        write(value and 0xFF)
        write((value ushr 8) and 0xFF)
    }

    private fun ByteArrayOutputStream.writeInt(value: Int) {
        writeShort(value and 0xFFFF)
        writeShort((value ushr 16) and 0xFFFF)
    }

    private companion object {
        const val LOCAL_HEADER_SIGNATURE = 0x04034B50
        const val CENTRAL_HEADER_SIGNATURE = 0x02014B50
        const val END_OF_CENTRAL_DIRECTORY_SIGNATURE = 0x06054B50
        const val VERSION_NEEDED = 20
        const val VERSION_MADE_BY = 20
        const val METHOD_STORED = 0

        /** Bit 11 of the general purpose flags: "the name is UTF-8". */
        const val UTF8_NAME_FLAG = 1 shl 11
        const val MS_DOS_DIRECTORY_ATTRIBUTE = 0x10
    }
}
