package com.etozhesandy.redpanda.core.archive.parse

import com.etozhesandy.redpanda.core.archive.format.DetectedFormat
import com.etozhesandy.redpanda.core.archive.parse.html.HtmlDialect
import com.etozhesandy.redpanda.core.archive.parse.html.HtmlDialogArchiveParser
import com.etozhesandy.redpanda.core.archive.parse.html.dialect.B00mHtmlDialect
import com.etozhesandy.redpanda.core.archive.parse.html.dialect.TorrentVkHtmlDialect
import com.etozhesandy.redpanda.core.archive.parse.html.dialect.VkClassicHtmlDialect
import com.etozhesandy.redpanda.core.archive.parse.vk.VkApiArchiveParser
import com.etozhesandy.redpanda.core.common.dispatcher.DefaultDispatcher
import com.etozhesandy.redpanda.core.common.dispatcher.IoDispatcher
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Picks the parser for a detected layout. Returns null for layouts that carry no dialog history at
 * all — those still import as a media library, so "no parser" is a normal outcome, not a failure.
 *
 * The HTML dialects are stateless, so the parsers wrapping them are constructed here rather than
 * through a Dagger multibinding, which would cost more than four parsers are worth — the dispatchers
 * the parser needs are injected into this factory and handed on.
 */
class ChatArchiveParserFactory @Inject constructor(
    private val vkApiParser: VkApiArchiveParser,
    private val classicDialect: VkClassicHtmlDialect,
    private val b00mDialect: B00mHtmlDialect,
    private val torrentDialect: TorrentVkHtmlDialect,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) {

    fun create(format: DetectedFormat): ChatArchiveParser? = when (format) {
        DetectedFormat.VK_HTML_CLASSIC -> htmlParser(classicDialect)
        DetectedFormat.VK_HTML_B00M -> htmlParser(b00mDialect)
        DetectedFormat.VK_API -> vkApiParser
        DetectedFormat.VK_HTML_TORRENT -> htmlParser(torrentDialect)
        DetectedFormat.MEDIA_ONLY -> null
    }

    private fun htmlParser(dialect: HtmlDialect) =
        HtmlDialogArchiveParser(dialect, ioDispatcher, defaultDispatcher)
}
