package com.etozhesandy.redpanda.core.archive.parse.vk

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Readers for the VK API dump's JSON, which is inconsistent about key casing: the same export
 * writes `document.Title` and `audio.title`, `link.Uri` and `photo.url`. Every accessor therefore
 * takes several candidate keys and returns the first one that is actually there, and returns null
 * rather than throwing when a value turns out to be an object where a string was expected.
 */

internal fun JsonObject.str(vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
    runCatching { this[key]?.jsonPrimitive?.contentOrNull }.getOrNull()?.takeIf { it.isNotBlank() }
}

internal fun JsonObject.long(vararg keys: String): Long? = keys.firstNotNullOfOrNull { key ->
    runCatching { this[key]?.jsonPrimitive?.longOrNull }.getOrNull()
}

internal fun JsonObject.obj(vararg keys: String): JsonObject? = keys.firstNotNullOfOrNull { key ->
    runCatching { this[key]?.jsonObject }.getOrNull()
}

internal fun JsonObject.arr(vararg keys: String): JsonArray? = keys.firstNotNullOfOrNull { key ->
    runCatching { this[key]?.jsonArray }.getOrNull()
}

/**
 * URL of the largest entry in a VK `sizes`/`Images` array — the copy worth keeping, since the
 * others are just downscales of it.
 */
internal fun largestImageUrl(sizes: JsonArray?): String? = sizes
    ?.mapNotNull { runCatching { it.jsonObject }.getOrNull() }
    ?.maxByOrNull { size ->
        val width = size["width"]?.jsonPrimitive?.intOrNull ?: 0
        val height = size["height"]?.jsonPrimitive?.intOrNull ?: 0
        width.toLong() * height.toLong()
    }
    ?.str("url", "src")

/**
 * Strips the casing and separators VK is inconsistent about, so `audio_playlist` and
 * `audioplaylist` — both of which occur, in different dumps — resolve to one branch.
 */
internal fun normalizeAttachmentType(type: String): String = type.lowercase().replace("_", "")

/** `mm:ss`, or `h:mm:ss` past an hour. Used for call and audio durations. */
internal fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainder = seconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, remainder)
    } else {
        "%d:%02d".format(minutes, remainder)
    }
}
