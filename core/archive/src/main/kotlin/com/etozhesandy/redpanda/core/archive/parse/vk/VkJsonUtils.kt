package com.etozhesandy.redpanda.core.archive.parse.vk

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
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

internal fun JsonObject.int(vararg keys: String): Int? = keys.firstNotNullOfOrNull { key ->
    runCatching { this[key]?.jsonPrimitive?.intOrNull }.getOrNull()
}

internal fun JsonObject.bool(vararg keys: String): Boolean? = keys.firstNotNullOfOrNull { key ->
    runCatching { this[key]?.jsonPrimitive?.booleanOrNull }.getOrNull()
}

internal fun JsonObject.obj(vararg keys: String): JsonObject? = keys.firstNotNullOfOrNull { key ->
    runCatching { this[key]?.jsonObject }.getOrNull()
}

internal fun JsonObject.arr(vararg keys: String): JsonArray? = keys.firstNotNullOfOrNull { key ->
    runCatching { this[key]?.jsonArray }.getOrNull()
}

/**
 * This element as an object, or null for every other shape — `JsonNull` included.
 *
 * The trap this exists to close: a VK dump writes an absent field as an explicit `null` rather
 * than by leaving the key out, and `JsonNull` is a perfectly ordinary [JsonElement], so
 * `element?.jsonObject` does **not** short-circuit on it — it throws `IllegalArgumentException`.
 * One real export writes `"reply_message": null` on all 517 136 of its messages, which threw on
 * the first message of the first dialog and failed the entire import with zero dialogs saved.
 */
internal fun JsonElement?.asObject(): JsonObject? = this as? JsonObject

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

/**
 * These exports save each JSON payload as a JS assignment — `messages=[...]` in the API-dump
 * layout, `let dialogjson = {...}` in the JSON-dump one — so the leading binding is peeled off
 * before the rest is parsed. The 40-character bound keeps a `=` inside actual JSON content from
 * being mistaken for one: no real binding is longer, and the first `=` of a bare payload is far
 * deeper in.
 */
internal fun stripJsAssignment(raw: String): String {
    val trimmed = raw.trim()
    val eq = trimmed.indexOf('=')
    return if (eq in 1..40) trimmed.substring(eq + 1).trim().removeSuffix(";") else trimmed
}

/**
 * A person's display name, or a community's. Both key casings occur: the API dump writes
 * `first_name`/`last_name`, the JSON dump `firstName`/`lastName` — and a group carries a single
 * `name` instead of either.
 */
internal fun personName(entity: JsonObject): String? {
    val name = listOfNotNull(
        entity.str("first_name", "firstName"),
        entity.str("last_name", "lastName"),
    ).joinToString(" ").trim()
    return name.ifBlank { entity.str("name") }
}

/** `https://vk.com/{kind}{ownerId}_{id}` — how VK addresses a post or a video on the web. */
internal fun vkPermalink(kind: String, ownerId: Long?, id: Long?): String =
    if (ownerId == null || id == null) "" else "https://vk.com/$kind${ownerId}_$id"

/**
 * A sticker's image. VK exports identify a sticker by number only, with no URL anywhere in the
 * dump, but its artwork is served under a stable public path — so the id is enough to show it.
 */
internal fun vkStickerUrl(stickerId: Long): String = "https://vk.com/sticker/1-$stickerId-512"

/** The export splits a document's name from its extension, and sometimes repeats it in both. */
internal fun documentName(body: JsonObject): String? {
    val title = body.str("Title", "title") ?: return null
    val extension = body.str("Ext", "ext") ?: return title
    return if (title.endsWith(".$extension", ignoreCase = true)) title else "$title.$extension"
}

/**
 * This element as a bare id. The JSON-dump layout stores a quote or a forward as either the id of
 * another message or the message itself, so the two shapes have to be told apart before either is
 * read — and `JsonNull` is a [JsonPrimitive] too, hence the explicit exclusion.
 */
internal fun JsonElement?.asId(): String? = (this as? JsonPrimitive)
    ?.takeIf { it !is JsonNull }
    ?.contentOrNull
    ?.takeIf { it.isNotBlank() }
