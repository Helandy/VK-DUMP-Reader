package com.etozhesandy.redpanda.core.archive.parse.vk

import com.etozhesandy.redpanda.core.archive.format.probe.DumpDirectoryNames.childNamed
import com.etozhesandy.redpanda.core.archive.format.probe.DumpDirectoryNames.dialogJsonFiles
import java.io.File

/**
 * Where the JSON dumper's files are, in either of the two builds it ships.
 *
 * Both write the same schema — abbreviated camelCase fields, one file per dialog, a `profiles`
 * array beside the messages — and differ only in what the files are *called* and whether the JSON
 * is wrapped in a `let dialogjson = …` assignment, which [VkJsonUtils.stripJsAssignment] already
 * peels. Detection and parsing therefore share this one table instead of the parser hardcoding the
 * English names, which is why the Russian build imported as a media-only profile for so long.
 *
 * `companions.json` is the awkward one: the English build keeps it in `json/`, the Russian build
 * keeps it *inside* the dialogs directory next to the `{peerId}.json` files — which is also why
 * dialog files have to be filtered by name rather than by extension.
 */
internal object VkJsonDumpPaths {

    /** The `json/` directory of the export rooted at [contentRoot], or null when there is none. */
    fun jsonRoot(contentRoot: File): File? = contentRoot.childNamed(JSON_DIR)?.takeIf { it.isDirectory }

    /** The per-dialog directory, looked up from the export root. */
    fun dialogsDir(contentRoot: File): File? = jsonRoot(contentRoot)?.let(::dialogsDirIn)

    fun dialogsDirIn(jsonRoot: File): File? =
        jsonRoot.childNamed("dialogs", "conversations")?.takeIf { it.isDirectory }

    /** The `{peerId}.json` files, and nothing else that happens to live beside them. */
    fun dialogFiles(dialogsDir: File): List<File> = dialogsDir.dialogJsonFiles()

    fun profileFile(jsonRoot: File): File? = jsonRoot.childNamed("profile.json", "страница.json")?.takeIf { it.isFile }

    fun friendsFile(jsonRoot: File): File? = jsonRoot.childNamed("friends.json", "друзья.json")?.takeIf { it.isFile }

    fun groupsFile(jsonRoot: File): File? = jsonRoot.childNamed("groups.json", "группы.json")?.takeIf { it.isFile }

    fun attachmentsFile(jsonRoot: File): File? =
        jsonRoot.childNamed("attachments.json", "вложения.json")?.takeIf { it.isFile }

    fun companionsFile(jsonRoot: File, dialogsDir: File): File? =
        (jsonRoot.childNamed("companions.json", "собеседники.json")
            ?: dialogsDir.childNamed("companions.json", "собеседники.json"))
            ?.takeIf { it.isFile }

    private const val JSON_DIR = "json"
}
