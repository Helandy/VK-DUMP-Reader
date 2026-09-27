package com.etozhesandy.redpanda.core.archive.parse.vk

import java.io.BufferedInputStream
import java.io.InputStream

/**
 * A payload file as bare JSON bytes: the leading JS binding (`messages=`, `let dialogjson =`) is
 * skipped and a trailing run of `;` and whitespace is dropped, without ever holding the file in
 * memory — a string copy of a tens-of-megabytes payload is what ran imports out of heap.
 *
 * The binding rule matches the one this replaced: after leading whitespace, the first `=` counts as
 * a binding only within [MAX_BINDING_LENGTH] bytes, so a `=` inside real JSON content is never
 * mistaken for one — no real binding is longer, and the first `=` of a bare payload is far deeper in.
 */
internal class JsAssignmentStream(source: InputStream) : InputStream() {

    private val input = BufferedInputStream(source, BUFFER_SIZE)
    private val chunk = ByteArray(BUFFER_SIZE)

    /** Bytes ready to hand out. */
    private var out = ByteArray(0)
    private var outPos = 0

    /** A trailing run of `;`/whitespace held back until it is known whether more content follows. */
    private var held = ByteArray(0)
    private var eof = false

    init {
        skipBinding()
    }

    override fun read(): Int {
        if (!ensureOutput()) return -1
        return out[outPos++].toInt() and 0xFF
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (len == 0) return 0
        if (!ensureOutput()) return -1
        val count = minOf(len, out.size - outPos)
        System.arraycopy(out, outPos, b, off, count)
        outPos += count
        return count
    }

    override fun close() = input.close()

    private fun ensureOutput(): Boolean {
        while (outPos >= out.size) {
            if (eof) return false
            val read = input.read(chunk)
            if (read == -1) {
                // Whatever is still held is the file's tail: `;` and whitespace only.
                eof = true
                held = ByteArray(0)
                return false
            }
            val combined = held + chunk.copyOf(read)
            var tailStart = combined.size
            while (tailStart > 0 && combined[tailStart - 1].isTailByte()) tailStart--
            out = combined.copyOfRange(0, tailStart)
            held = combined.copyOfRange(tailStart, combined.size)
            outPos = 0
        }
        return true
    }

    private fun skipBinding() {
        // Leading whitespace goes either way: JSON ignores it, and the binding rule starts after it.
        while (true) {
            input.mark(1)
            val next = input.read()
            if (next == -1) return
            if (!next.toByte().isWhitespace()) {
                input.reset()
                break
            }
        }
        input.mark(MAX_BINDING_LENGTH + 1)
        val head = ByteArray(MAX_BINDING_LENGTH + 1)
        var filled = 0
        while (filled < head.size) {
            val read = input.read(head, filled, head.size - filled)
            if (read == -1) break
            filled += read
        }
        input.reset()
        val eq = (0 until filled).firstOrNull { head[it] == '='.code.toByte() }
        if (eq != null && eq in 1..MAX_BINDING_LENGTH) input.skipFully(eq + 1L)
    }

    private fun InputStream.skipFully(count: Long) {
        var remaining = count
        while (remaining > 0) {
            val skipped = skip(remaining)
            if (skipped <= 0) {
                if (read() == -1) return
                remaining--
            } else {
                remaining -= skipped
            }
        }
    }

    private fun Byte.isWhitespace(): Boolean =
        this == ' '.code.toByte() || this == '\n'.code.toByte() ||
            this == '\r'.code.toByte() || this == '\t'.code.toByte()

    private fun Byte.isTailByte(): Boolean = this == ';'.code.toByte() || isWhitespace()

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
        const val MAX_BINDING_LENGTH = 40
    }
}
