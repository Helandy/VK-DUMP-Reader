package com.etozhesandy.redpanda.core.archive.extract

import java.io.File
import java.nio.charset.Charset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Covers the entry-name decoding in [ZipExtraction] against the archive shapes that actually
 * occur, all of which were verified against real VK exports before being written down here.
 */
class ZipExtractionTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val cp866: Charset = Charset.forName("CP866")

    private fun extract(writer: TestZipWriter): File {
        val archive = writer.writeTo(temporaryFolder.newFile("archive.zip"))
        val destination = temporaryFolder.newFolder("out")
        ZipExtraction.extract(archive, destination)
        return destination
    }

    /** Paths of every extracted file, relative to the destination and `/`-separated. */
    private fun File.extractedFiles(): Set<String> = walkTopDown()
        .filter { it.isFile }
        .map { it.relativeTo(this).path.replace(File.separatorChar, '/') }
        .toSet()

    /**
     * The regression that lost every dialog of one 270-dialog export.
     *
     * Windows zip tools write names in the OEM codepage and set the UTF-8 flag only on the names
     * that codepage cannot hold, so one archive carries both. Applying a single charset to all of
     * them — which is what `ZipFile.setCharset` does, flag or no flag — has to corrupt one group
     * or the other.
     */
    @Test
    fun `cp866 and utf8-flagged names in one archive both decode correctly`() {
        val destination = extract(
            TestZipWriter()
                .addUnflagged("Профиль/Диалоги/", cp866)
                .addUnflagged("Профиль/Диалоги/Иван Петров (id1)/history_1.html", cp866, "иван")
                .addUtf8Flagged("Профиль/Диалоги/Michał Tabaka (id2)/history_1.html", "michal"),
        )

        assertEquals(
            setOf(
                "Профиль/Диалоги/Иван Петров (id1)/history_1.html",
                "Профиль/Диалоги/Michał Tabaka (id2)/history_1.html",
            ),
            destination.extractedFiles(),
        )
    }

    /** A single unflagged legacy name must not drag the flagged ones onto its codepage. */
    @Test
    fun `one unflagged cp866 name does not corrupt the utf8-flagged majority`() {
        val destination = extract(
            TestZipWriter()
                .addUnflagged("Профиль/МАТ/", cp866)
                .addUtf8Flagged("Профиль/Диалоги/Парни/Артём (id1)/history_1.html", "a")
                .addUtf8Flagged("Профиль/Диалоги/Девушки/Алина (id2)/history_1.html", "b"),
        )

        assertEquals(
            setOf(
                "Профиль/Диалоги/Парни/Артём (id1)/history_1.html",
                "Профиль/Диалоги/Девушки/Алина (id2)/history_1.html",
            ),
            destination.extractedFiles(),
        )
    }

    /**
     * Handed a *directory* header, zip4j extracts that whole subtree and rebuilds each name with
     * `String.replaceFirst`, whose first argument is a regex — so a folder whose name contains
     * regex metacharacters never matches itself and its subtree lands under the raw CP437 name.
     * `+` and `(`/`)` are both ordinary characters in the folder names real exports use.
     */
    @Test
    fun `a directory name containing regex metacharacters is still decoded`() {
        val destination = extract(
            TestZipWriter()
                .addUnflagged("Профиль/инцест+тройнички (id1)/", cp866)
                .addUnflagged("Профиль/инцест+тройнички (id1)/history_1.html", cp866, "x"),
        )

        assertEquals(
            setOf("Профиль/инцест+тройнички (id1)/history_1.html"),
            destination.extractedFiles(),
        )
        assertEquals(listOf("Профиль"), destination.list()?.toList())
    }

    /** `zip` on macOS and Linux writes UTF-8 bytes but never sets the flag. */
    @Test
    fun `utf8 bytes written without the flag are decoded as utf8`() {
        val destination = extract(
            TestZipWriter().addUnflaggedUtf8Bytes("Профиль/Диалоги/Иван (id1)/history_1.html", "x"),
        )

        assertEquals(setOf("Профиль/Диалоги/Иван (id1)/history_1.html"), destination.extractedFiles())
    }

    /** Nothing to repair: zip4j already decodes flagged names correctly on its own. */
    @Test
    fun `an archive that flags every name is left to zip4j`() {
        val destination = extract(
            TestZipWriter()
                .addUtf8Flagged("Профиль/Диалоги/")
                .addUtf8Flagged("Профиль/Диалоги/Иван (id1)/history_1.html", "x"),
        )

        assertEquals(setOf("Профиль/Диалоги/Иван (id1)/history_1.html"), destination.extractedFiles())
    }

    @Test
    fun `an ascii-only archive is extracted unchanged`() {
        val destination = extract(
            TestZipWriter().addUnflagged("profile/messages/1.json", Charsets.US_ASCII, "{}"),
        )

        assertEquals(setOf("profile/messages/1.json"), destination.extractedFiles())
    }

    @Test
    fun `file contents survive the rename`() {
        val destination = extract(
            TestZipWriter().addUnflagged("Профиль/Диалоги/Иван (id1)/history_1.html", cp866, "привет"),
        )

        assertEquals(
            "привет",
            File(destination, "Профиль/Диалоги/Иван (id1)/history_1.html").readText(),
        )
    }

    /**
     * The name is ours to build once the codepage is applied, so containment is checked here
     * rather than left to zip4j — and a traversal entry cannot be waved through just because the
     * rest of the archive needed repairing.
     */
    @Test
    fun `an entry that escapes the destination is skipped`() {
        val destination = extract(
            TestZipWriter()
                .addUnflagged("Профиль/Диалоги/Иван (id1)/history_1.html", cp866, "ok")
                .addUnflagged("../escaped.html", cp866, "evil"),
        )

        assertEquals(setOf("Профиль/Диалоги/Иван (id1)/history_1.html"), destination.extractedFiles())
        assertFalse(File(destination.parentFile, "escaped.html").exists())
    }

    /** Empty directories carry no data, but the tree they describe still has to come out right. */
    @Test
    fun `directory entries are created`() {
        val destination = extract(
            TestZipWriter()
                .addUnflagged("Профиль/", cp866)
                .addUnflagged("Профиль/Вложения/", cp866),
        )

        assertTrue(File(destination, "Профиль/Вложения").isDirectory)
    }
}
