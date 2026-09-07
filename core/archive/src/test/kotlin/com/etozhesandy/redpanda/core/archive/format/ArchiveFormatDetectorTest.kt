package com.etozhesandy.redpanda.core.archive.format

import com.etozhesandy.redpanda.core.archive.format.probe.VkApiLayoutProbe
import com.etozhesandy.redpanda.core.archive.format.probe.VkHtmlHistoryLayoutProbe
import com.etozhesandy.redpanda.core.archive.format.probe.VkHtmlTorrentLayoutProbe
import com.etozhesandy.redpanda.core.archive.format.probe.VkJsonDumpLayoutProbe
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Each case is named after the real archive it reproduces.
 *
 * The point of the suite is not coverage of the detector's branches; it is that the corpus keeps
 * disagreeing with itself. Teaching the app one export shape repeatedly stopped it recognising
 * another, and nothing failed when it happened — the archive simply imported as a media-only
 * profile with no dialogs in it. These fixtures make that a red test instead.
 */
class ArchiveFormatDetectorTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val detector = ArchiveFormatDetector(
        setOf(
            VkApiLayoutProbe(),
            VkJsonDumpLayoutProbe(),
            VkHtmlTorrentLayoutProbe(),
            VkHtmlHistoryLayoutProbe(HtmlDialectSniffer()),
        ),
    )

    @Test
    fun `finds an api dump three directories down - test56`() {
        val tree = tree()
        tree.vkApiDump("Дамп/Дамп (id1001)/Дамп (id1001)")

        assertLayouts(
            listOf(
                DetectedFormat.VK_API to
                    "Дамп/Дамп (id1001)/Дамп (id1001)",
            ),
        )
    }

    @Test
    fun `finds a json dump with english names - test25`() {
        tree().jsonDump("Экспорт 1001")

        assertLayouts(listOf(DetectedFormat.VK_JSON_DUMP to "Экспорт 1001"))
    }

    @Test
    fun `finds a json dump with russian names - test01`() {
        tree().jsonDump(
            at = "Экспорт 1002",
            dialogsDir = "conversations",
            profileFile = "страница.json",
            companionsInDialogsDir = true,
        )

        assertLayouts(listOf(DetectedFormat.VK_JSON_DUMP to "Экспорт 1002"))
    }

    @Test
    fun `finds a json dump whose profile file is missing`() {
        tree().jsonDump(at = "json only", profileFile = null)

        assertLayouts(listOf(DetectedFormat.VK_JSON_DUMP to "json only"))
    }

    @Test
    fun `companions file alone is not a dialog - test01`() {
        val tree = tree()
        tree.file("Экспорт/json/conversations/companions.json", "[]")
        tree.mediaOnly("Экспорт")

        assertEquals(emptyList<ArchiveLayout>(), detector.detectAll(root))
    }

    @Test
    fun `prefers the json copy over the html copy of the same dump - test42`() {
        val tree = tree()
        tree.jsonDump(at = "Экспорт 1003", dialogsDir = "conversations")
        tree.torrentDump("Экспорт 1003/html")

        assertLayouts(listOf(DetectedFormat.VK_JSON_DUMP to "Экспорт 1003"))
    }

    @Test
    fun `joins the two halves of a dump split across nested archives - test28`() {
        val tree = tree()
        // What the html.zip / json.zip pair looks like once the wrappers have been flattened away.
        tree.jsonDump(at = "Экспорт 1004")
        tree.torrentDump("Экспорт 1004/html")

        assertLayouts(listOf(DetectedFormat.VK_JSON_DUMP to "Экспорт 1004"))
    }

    @Test
    fun `keeps looking below an html dump for a richer one - test09`() {
        val tree = tree()
        tree.htmlHistoryDump("Экспорт (id1005)")
        tree.vkApiDump("Экспорт (id1005)/вложенный дамп")

        val layouts = detector.detectAll(root)
        assertEquals(1, layouts.size)
        assertEquals(DetectedFormat.VK_API, layouts.single().format)
        assertEquals(
            "Экспорт (id1005)/вложенный дамп",
            layouts.single().contentRoot.relativeTo(root).path,
        )
        // The whole person folder is the dump, not just the subfolder the parser reads.
        assertEquals("Экспорт (id1005)", layouts.single().dumpRoot.relativeTo(root).path)
    }

    @Test
    fun `keeps unrelated dumps apart - test12`() {
        val tree = tree()
        tree.vkApiDump("Экспорт А (id1006)")
        tree.vkApiDump("Экспорт Б")
        tree.htmlHistoryDump("Экспорт В")

        assertEquals(3, detector.detectAll(root).size)
    }

    @Test
    fun `reads html history with no Диалоги wrapper - test27`() {
        tree().htmlHistoryDump("Экспорт 1007", wrapper = null)

        assertLayouts(listOf(DetectedFormat.VK_HTML_CLASSIC to "Экспорт 1007"))
    }

    @Test
    fun `tells the b00m dialect from the classic one - test16`() {
        tree().htmlHistoryDump("Экспорт (id1008)", b00m = true)

        assertLayouts(listOf(DetectedFormat.VK_HTML_B00M to "Экспорт (id1008)"))
    }

    @Test
    fun `finds Переписки under a Диалоги folder - test37`() {
        tree().torrentDump("Экспорт (id1009)/Экспорт (id1009)/Диалоги")

        assertLayouts(
            listOf(
                DetectedFormat.VK_HTML_TORRENT to
                    "Экспорт (id1009)/Экспорт (id1009)/Диалоги",
            ),
        )
    }

    @Test
    fun `an empty marker directory is not a dump`() {
        val tree = tree()
        tree.dir("Пусто/Диалоги")
        tree.dir("Пусто/Переписки")
        tree.mediaOnly("Пусто")

        assertEquals(emptyList<ArchiveLayout>(), detector.detectAll(root))
    }

    @Test
    fun `a folder of photos imports as one media profile - test24`() {
        val tree = tree()
        tree.mediaOnly("Сборник 1/Медиа")
        tree.mediaOnly("Сборник 2/Медиа")

        assertEquals(emptyList<ArchiveLayout>(), detector.detectAll(root))
        assertEquals(DetectedFormat.MEDIA_ONLY, detector.detect(root).format)
    }

    private val root: File get() = temporaryFolder.root

    private fun tree() = TestDumpTree(root)

    private fun assertLayouts(expected: List<Pair<DetectedFormat, String>>) {
        val actual = detector.detectAll(root).map { it.format to it.contentRoot.relativeTo(root).path }
        assertEquals(expected, actual)
    }
}
