package com.etozhesandy.redpanda.core.archive.extract

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * The wrapper directory a nested archive unpacks into is what made one export import as two
 * profiles, and what made another import twice over. Both shapes are real, both are here.
 */
class ArchiveFlatteningTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    /**
     * The split dump. `html.zip` and `json.zip` sit side by side and hold the two halves of one
     * export; leaving each in its own wrapper left the detector with two directories where neither
     * contained the other, so it reported two unrelated dumps.
     */
    @Test
    fun `promotes the only directory a wrapper holds`() {
        val person = temporaryFolder.newFolder("Экспорт")
        val wrapper = File(person, "json.zip.extracted").apply { mkdirs() }
        File(wrapper, "json/dialogs").mkdirs()

        val result = ArchiveFlattening.flatten(wrapper)

        assertEquals(File(person, "json"), result)
        assertTrue(File(person, "json/dialogs").isDirectory)
        assertFalse(wrapper.exists())
    }

    /** `Архив.zip` holds `html/` and `json/` together, which is the export's own shape. */
    @Test
    fun `leaves a wrapper holding more than one directory alone`() {
        val person = temporaryFolder.newFolder("Экспорт")
        val wrapper = File(person, "Архив.zip.extracted").apply { mkdirs() }
        File(wrapper, "html").mkdirs()
        File(wrapper, "json").mkdirs()

        assertEquals(wrapper, ArchiveFlattening.flatten(wrapper))
        assertTrue(File(wrapper, "html").isDirectory)
    }

    /**
     * The dump shipped twice: a folder unpacked, and an archive of that same folder beside it.
     * Keeping both made one export import as two profiles.
     */
    @Test
    fun `drops the unpacked copy when the folder beside it is already as complete`() {
        val person = temporaryFolder.newFolder("Сборник")
        val existing = File(person, "Дамп").apply { mkdirs() }
        File(existing, "json/dialogs").mkdirs()
        File(existing, "json/dialogs/1.json").writeText("{}")
        val wrapper = File(person, "Дамп.rar.extracted").apply { mkdirs() }
        File(wrapper, "Дамп/json/dialogs").mkdirs()

        val result = ArchiveFlattening.flatten(wrapper)

        assertEquals(existing, result)
        assertFalse(wrapper.exists())
        assertTrue(File(existing, "json/dialogs/1.json").isFile)
    }

    /** A folder left half-extracted by whoever built the archive must not shadow the real thing. */
    @Test
    fun `replaces a thinner folder with the archive's own copy`() {
        val person = temporaryFolder.newFolder("Сборник")
        File(person, "Дамп").mkdirs()
        val wrapper = File(person, "Дамп.rar.extracted").apply { mkdirs() }
        File(wrapper, "Дамп/json/dialogs").mkdirs()
        File(wrapper, "Дамп/json/dialogs/1.json").writeText("{}")

        val result = ArchiveFlattening.flatten(wrapper)

        assertEquals(File(person, "Дамп"), result)
        assertTrue(File(person, "Дамп/json/dialogs/1.json").isFile)
        assertFalse(wrapper.exists())
    }

    /** A name taken by a file is a coincidence, not a duplicate: there is nothing safe to merge. */
    @Test
    fun `leaves a wrapper alone when the name is taken by a file`() {
        val person = temporaryFolder.newFolder("Человек")
        File(person, "Дамп").writeText("not a directory")
        val wrapper = File(person, "Дамп.rar.extracted").apply { mkdirs() }
        File(wrapper, "Дамп").mkdirs()

        assertEquals(wrapper, ArchiveFlattening.flatten(wrapper))
        assertTrue(File(wrapper, "Дамп").isDirectory)
    }
}
