package com.etozhesandy.redpanda.core.model

/**
 * Media kind of an [Attachment].
 *
 * Constants may be **added** but never renamed or removed: `StorageTypeConverters` resolves stored
 * rows by name, and existing databases hold every name ever written.
 *
 * [STICKER] and [GRAFFITI] are images with real URLs but are kept apart from [PHOTO] on purpose —
 * one real export holds 890 stickers against 3 166 actual photos, and folding them together buries
 * the photos in the media grid. [OTHER] is the catch-all for kinds carrying no media of their own
 * (stories, gifts, playlists, market items, and anything a future export invents), so an
 * unrecognised attachment is still recorded instead of silently dropped.
 */
enum class AttachmentType {
    PHOTO,
    VIDEO,
    FILE,
    AUDIO,
    STICKER,
    GRAFFITI,
    LINK,
    WALL,
    CALL,
    OTHER,
}
