package com.etozhesandy.redpanda.core.model

/**
 * A media item found in an imported archive.
 *
 * [path] is either a `file://` path inside the profile's directory or a remote `http(s)` URL —
 * some export formats (e.g. VK's HTML export) only reference media on the source's CDN rather
 * than downloading it. [messageId] is null when the source format has no way to correlate the
 * attachment back to the message it was sent in (true for that same VK export format, whose
 * attachment galleries are flat and unlinked).
 *
 * [path] may also be **blank**: some attachments are pure metadata with nothing to open — a track
 * an export names but doesn't include, a call, a video the source has since removed. Anything that
 * renders or downloads a [path] has to handle that.
 *
 * [caption] is what to show when there is no thumbnail to show: a document's file name, an audio
 * track's artist and title, a link's title, an excerpt of a wall post. Null when the media speaks
 * for itself.
 *
 * [sourceFolder] is the folder the file lives in inside the archive, relative to its content root.
 * It is set for every attachment backed by a file the import actually holds — those the parser
 * resolved to a downloaded copy, and those found by scanning for media no parser referenced (see
 * `OrphanMediaScanner`) — and is what lists a file among the archive's own media. Null for an
 * attachment that only points at a remote URL, which is no file of the archive's.
 */
data class Attachment(
    val id: String,
    val messageId: String?,
    val dialogId: String,
    val profileId: String,
    val type: AttachmentType,
    val path: String,
    val orderInMessage: Int,
    val timestampEpoch: Long,
    val caption: String? = null,
    val sourceFolder: String? = null,
) {
    companion object {
        fun List<Attachment>.sortedBy(sort: MediaSort, ascending: Boolean): List<Attachment> {
            val comparator: Comparator<Attachment> = when (sort) {
                // Attachments of one message share a timestamp, so their in-message order breaks the tie.
                MediaSort.DATE -> compareBy<Attachment> { it.timestampEpoch }.thenBy { it.orderInMessage }
                MediaSort.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.path.substringAfterLast('/') }
            }
            return sortedWith(if (ascending) comparator else comparator.reversed())
        }
    }
}
