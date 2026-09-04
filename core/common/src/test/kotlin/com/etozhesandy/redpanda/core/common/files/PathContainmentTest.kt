package com.etozhesandy.redpanda.core.common.files

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PathContainmentTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val root: File get() = temporaryFolder.root.resolve("profiles").apply { mkdirs() }

    @Test
    fun `a file under the root is inside it`() {
        assertTrue(File(root, "abc/raw/photo.jpg").isInside(root))
    }

    @Test
    fun `traversal out of the root is not inside it`() {
        assertFalse(File(root, "../../databases/redpanda.db").isInside(root))
        assertFalse(File(temporaryFolder.root, "databases/redpanda.db").isInside(root))
    }

    @Test
    fun `the root itself is not inside itself`() {
        assertFalse(root.isInside(root))
    }

    /** Canonical resolution is what makes this hold: an archive can write symlinks. */
    @Test
    fun `a symlink pointing out of the root is not inside it`() {
        val outside = temporaryFolder.newFile("outside.txt")
        val link = File(root, "link.txt")
        Files.createSymbolicLink(link.toPath(), outside.toPath())
        assertFalse(link.isInside(root))
    }
}
