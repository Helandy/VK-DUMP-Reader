package com.etozhesandy.redpanda.core.archive.format

/**
 * Layouts real VK exports come in. The three `VK_HTML_*` values share the same folder structure
 * (`Диалоги/{категория}/{Имя (idN)}/history_N.html`) but carry completely different markup, so they
 * are told apart by sniffing a history file rather than by paths alone — see [HtmlDialectSniffer].
 *
 * [MEDIA_ONLY] is not a failure: plenty of real dumps are just a folder of photos and videos with
 * no dialog history at all, and those import fine as a media library.
 */
enum class DetectedFormat {
    VK_HTML_CLASSIC,
    VK_HTML_B00M,
    VK_HTML_TORRENT,
    VK_API,
    MEDIA_ONLY,
}
