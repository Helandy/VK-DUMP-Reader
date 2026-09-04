package com.etozhesandy.redpanda.core.archive.parse.html

import java.io.File
import java.util.UUID

/** Stateless helpers shared by the HTML dialects and [HtmlDialogArchiveParser]. */
object HtmlParseUtils {

    private val vkFolderNameRegex = Regex("""^(.*)\s\(id(\d+)\)$""")
    private val trailingIdFolderNameRegex = Regex("""^(.*?)\s+(\d+)$""")
    private val idRegex = Regex("""id(\d+)""")

    /** `Имя Фамилия (id123)` — the naming both `Диалоги/` exports use. */
    fun parseVkFolderName(name: String): HtmlContactFolder? {
        val match = vkFolderNameRegex.find(name) ?: return null
        return HtmlContactFolder(match.groupValues[1].trim(), match.groupValues[2])
    }

    /** `Имя Фамилия 123` — the `Переписки/` export drops the parentheses entirely. */
    fun parseTrailingIdFolderName(name: String): HtmlContactFolder? {
        val match = trailingIdFolderNameRegex.find(name) ?: return null
        return HtmlContactFolder(match.groupValues[1].trim(), match.groupValues[2])
    }

    /** Pulls `123` out of anything shaped like `https://vk.com/id123`. */
    fun extractVkId(text: String): String? = idRegex.find(text)?.groupValues?.get(1)

    /** Files in [dir] whose name matches [regex], ordered by the number in its first group. */
    fun numberedPages(dir: File, regex: Regex): List<File> =
        dir.listFiles { file -> file.isFile && regex.matches(file.name) }
            .orEmpty()
            .sortedBy { regex.find(it.name)?.groupValues?.get(1)?.toIntOrNull() ?: 0 }

    /**
     * A stable id for a message, so re-importing the same archive replaces rows instead of
     * duplicating them. The sequence number is part of the hash because a dialog can genuinely hold
     * several identical messages at the same second — 660 such pairs in one real export.
     */
    fun deterministicMessageId(
        dialogId: String,
        timestamp: Long,
        senderId: String,
        text: String,
        sequence: Int,
    ): String = UUID.nameUUIDFromBytes("$dialogId|$timestamp|$senderId|$text|$sequence".toByteArray()).toString()
}
