package com.etozhesandy.redpanda.features.chat.utils

/**
 * Length of an MP3 in milliseconds from its first bytes and its total size, or null when [head]
 * isn't an MP3 this can read.
 *
 * VK serves voice messages as constant-bitrate MP3s with no Xing/Info header, which leaves
 * Android's own `MediaMetadataRetriever` reporting a length of 0. The length still follows from
 * the size and the bitrate in the first frame header — the same estimate `ffprobe` makes. When a
 * Xing/Info header is present its frame count is used instead, which is exact for VBR files too.
 */
fun estimateMp3DurationMs(head: ByteArray, totalBytes: Long): Long? {
    val audioStart = id3v2Size(head)
    val frameStart = findFrameSync(head, audioStart) ?: return null
    val frame = Mp3FrameHeader.parse(head, frameStart) ?: return null

    xingFrameCount(head, frameStart, frame)?.let { frames ->
        return frames * frame.samplesPerFrame * 1000L / frame.sampleRate
    }
    val audioBytes = totalBytes - frameStart
    if (audioBytes <= 0) return null
    return audioBytes * 8 * 1000L / frame.bitrate
}

/** Size of a leading ID3v2 tag, header and footer included; 0 when there is none. */
private fun id3v2Size(bytes: ByteArray): Int {
    if (bytes.size < 10 || bytes[0] != 'I'.code.toByte() || bytes[1] != 'D'.code.toByte() || bytes[2] != '3'.code.toByte()) {
        return 0
    }
    // Sync-safe integer: seven bits per byte.
    val size = (bytes[6].toInt() and 0x7F shl 21) or (bytes[7].toInt() and 0x7F shl 14) or
        (bytes[8].toInt() and 0x7F shl 7) or (bytes[9].toInt() and 0x7F)
    val hasFooter = bytes[5].toInt() and 0x10 != 0
    return 10 + size + if (hasFooter) 10 else 0
}

private fun findFrameSync(bytes: ByteArray, from: Int): Int? {
    for (i in from until bytes.size - 3) {
        if (bytes[i].toInt() and 0xFF == 0xFF && bytes[i + 1].toInt() and 0xE0 == 0xE0 &&
            Mp3FrameHeader.parse(bytes, i) != null
        ) {
            return i
        }
    }
    return null
}

/** Frame count from a Xing/Info header in the first frame, if it has one that carries it. */
private fun xingFrameCount(bytes: ByteArray, frameStart: Int, frame: Mp3FrameHeader): Long? {
    val offset = frameStart + 4 + frame.sideInfoSize
    if (offset + 12 > bytes.size) return null
    val tag = String(bytes, offset, 4, Charsets.US_ASCII)
    if (tag != "Xing" && tag != "Info") return null
    val flags = bytes.readInt(offset + 4)
    if (flags and 0x1 == 0) return null
    return bytes.readInt(offset + 8).toLong() and 0xFFFFFFFFL
}

private fun ByteArray.readInt(at: Int): Int =
    (this[at].toInt() and 0xFF shl 24) or (this[at + 1].toInt() and 0xFF shl 16) or
        (this[at + 2].toInt() and 0xFF shl 8) or (this[at + 3].toInt() and 0xFF)

private class Mp3FrameHeader(
    val bitrate: Int,
    val sampleRate: Int,
    val samplesPerFrame: Int,
    val sideInfoSize: Int,
) {
    companion object {
        private val bitratesV1 = intArrayOf(0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320)
        private val bitratesV2 = intArrayOf(0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160)
        private val sampleRatesV1 = intArrayOf(44100, 48000, 32000)

        /** Layer III only — the one layer voice messages and music files come in. */
        fun parse(bytes: ByteArray, at: Int): Mp3FrameHeader? {
            if (at + 4 > bytes.size) return null
            val b1 = bytes[at + 1].toInt() and 0xFF
            val b2 = bytes[at + 2].toInt() and 0xFF
            val b3 = bytes[at + 3].toInt() and 0xFF
            val version = b1 shr 3 and 0x3 // 3 = MPEG-1, 2 = MPEG-2, 0 = MPEG-2.5
            val layer = b1 shr 1 and 0x3 // 1 = Layer III
            if (version == 1 || layer != 1) return null
            val bitrateIndex = b2 shr 4
            val sampleRateIndex = b2 shr 2 and 0x3
            if (bitrateIndex == 0 || bitrateIndex == 15 || sampleRateIndex == 3) return null

            val isV1 = version == 3
            val sampleRate = sampleRatesV1[sampleRateIndex] / when (version) {
                3 -> 1
                2 -> 2
                else -> 4
            }
            val isMono = b3 shr 6 == 3
            return Mp3FrameHeader(
                bitrate = (if (isV1) bitratesV1 else bitratesV2)[bitrateIndex] * 1000,
                sampleRate = sampleRate,
                samplesPerFrame = if (isV1) 1152 else 576,
                sideInfoSize = when {
                    isV1 -> if (isMono) 17 else 32
                    else -> if (isMono) 9 else 17
                },
            )
        }
    }
}
