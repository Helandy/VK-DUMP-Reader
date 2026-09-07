package com.etozhesandy.redpanda.core.archive.format

import java.io.File

/**
 * Builds the smallest directory tree that still looks like a real export to the detector.
 *
 * Each method reproduces one shape found in the archive corpus, so a test reads as the archive it
 * stands for rather than as a pile of `mkdirs` calls. Only what detection actually inspects is
 * written: marker directories, a peer-id-shaped file name, and — for the HTML exports, which share
 * a folder tree and share no markup — enough of a history page for the dialect sniffer to work on.
 */
internal class TestDumpTree(private val root: File) {

    /** `messages/{peerId}/{page}.json` beside the offline viewer that reads it. */
    fun vkApiDump(at: String): File = dir(at).also { dump ->
        file("$at/profile.json", """info={"id":1,"first_name":"Имя","last_name":"Фамилия"}""")
        file("$at/messages/conversations.json", """conversations={}""")
        file("$at/messages/56800555/1.json", """messages=[]""")
    }

    /**
     * `json/{dialogs|conversations}/{peerId}.json`. The English build of this dumper names the
     * directory `dialogs` and its profile file `profile.json`; the Russian one names them
     * `conversations` and `страница.json` and keeps `companions.json` inside the dialogs directory.
     */
    fun jsonDump(
        at: String,
        dialogsDir: String = "dialogs",
        profileFile: String? = "profile.json",
        companionsInDialogsDir: Boolean = false,
    ): File = dir(at).also { dump ->
        profileFile?.let { file("$at/json/$it", """{"id":1,"firstName":"Имя","lastName":"Фамилия"}""") }
        file("$at/json/$dialogsDir/98766500.json", """{"messages":[],"profiles":[]}""")
        if (companionsInDialogsDir) file("$at/json/$dialogsDir/companions.json", "[]")
    }

    /** `[Диалоги/]{категория}/{Имя (idN)}/history_N.html`, the wrapper directory optional. */
    fun htmlHistoryDump(at: String, wrapper: String? = "Диалоги", b00m: Boolean = false): File = dir(at).also {
        val base = listOfNotNull(at.ifEmpty { null }, wrapper).joinToString("/")
        val markup = if (b00m) B00M_PAGE else CLASSIC_PAGE
        file("$base/Девушки/Контакт (id101)/history_1.html", markup)
    }

    /** `Переписки/{категория}/{Имя NNNN}/{N}.html`, whose enclosing directory varies by export. */
    fun torrentDump(at: String): File = dir(at).also {
        val base = listOfNotNull(at.ifEmpty { null }, "Переписки").joinToString("/")
        file("$base/Девушки/Контакт 102/1.html", TORRENT_PAGE)
    }

    /** A folder of photos and videos and nothing else, which is a perfectly ordinary import. */
    fun mediaOnly(at: String): File = dir(at).also {
        file("${at.ifEmpty { "." }}/photo.jpg", "x")
        file("${at.ifEmpty { "." }}/video.mp4", "x")
    }

    fun dir(path: String): File =
        (if (path.isEmpty()) root else File(root, path)).apply { mkdirs() }

    fun file(path: String, content: String): File = File(root, path).apply {
        parentFile?.mkdirs()
        writeText(content)
    }

    private companion object {
        /** `div.m` / `a.ma`, with none of the b00m markers the sniffer keys on. */
        const val CLASSIC_PAGE =
            """<html><body><div class="m"><div class="mm">""" +
                """<a class="ma" href="https://vk.com/id101">Контакт</a>""" +
                """<span class="md">12.04.2021, 17:10:04</span></div>""" +
                """<div class="mc"><div class="mt">Привет</div></div></div></body></html>"""

        const val B00M_PAGE =
            """<html><body><div class="im_in"><div class="im_log_author_chat_name">""" +
                """<a class="mem_link" href="https://vk.com/id101">Контакт</a></div>""" +
                """<a class="im_date_link">12.04.2021 17:10</a>""" +
                """<div class="wrapped">Привет</div></div></body></html>"""

        const val TORRENT_PAGE =
            """<html><body><div class="conversation"><div class="message message-right">""" +
                """<div class="flex column gap-2">Привет<span>08.04.2023, 21:07:55</span>""" +
                """</div></div></div></body></html>"""
    }
}
