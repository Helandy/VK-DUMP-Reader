package com.etozhesandy.redpanda.core.archive.format.probe

import java.io.File
import java.text.Normalizer

/**
 * Name lookups every probe shares, so no probe reaches for `File(dir, "Диалоги")` on its own.
 *
 * Two things a plain `File(dir, name)` gets wrong on real exports:
 *  - **case**, because the same dumper ships `json/` and `JSON/` depending on the build;
 *  - **Unicode form**, because an archive written on macOS carries decomposed names, where `й` is
 *    `и` followed by a combining breve. None of today's marker names contains a decomposable
 *    letter, so this costs nothing now — it is here so that the first marker that does contain one
 *    fails loudly in a test rather than silently in someone's import.
 */
internal object DumpDirectoryNames {

    /** The first child of this directory matching any of [names], or null when none exists. */
    fun File.childNamed(vararg names: String): File? {
        for (name in names) {
            val direct = File(this, name)
            if (direct.exists()) return direct
        }
        val wanted = names.map { it.normalized() }.toSet()
        return listFiles().orEmpty().firstOrNull { it.name.normalized() in wanted }
    }

    /**
     * The `{peerId}.json` files of a dialogs directory.
     *
     * The numeric filter is not cosmetic: the Russian build of the JSON dumper keeps
     * `companions.json` *inside* the dialogs directory, so taking every `.json` file imported a
     * phantom dialog named `companions` and reported one dialog more than the export contains.
     */
    fun File.dialogJsonFiles(): List<File> = listFiles()
        .orEmpty()
        .filter { it.isFile && it.extension.equals("json", ignoreCase = true) && it.nameWithoutExtension.isPeerId() }

    /** Whether any subdirectory is named after a peer id, which is how per-peer folders are laid out. */
    fun File.hasPeerIdSubdirectory(): Boolean =
        listFiles().orEmpty().any { it.isDirectory && it.name.isPeerId() }

    /** Group peers are written negative, so the sign is part of a valid id. */
    fun String.isPeerId(): Boolean =
        isNotEmpty() && PEER_ID.matches(this)

    private fun String.normalized(): String = Normalizer.normalize(this, Normalizer.Form.NFC).lowercase()

    private val PEER_ID = Regex("""-?\d+""")
}
