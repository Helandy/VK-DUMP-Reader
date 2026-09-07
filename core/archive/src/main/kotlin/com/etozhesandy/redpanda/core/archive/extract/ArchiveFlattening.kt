package com.etozhesandy.redpanda.core.archive.extract

import java.io.File

/**
 * Removes the directory [ArchiveNormalizer] wraps a nested archive's contents in, once those
 * contents are on disk.
 *
 * The wrapper is an artefact of unpacking, and leaving it in place is what made one export import
 * as two profiles. A dumper that splits a dump across a `html.zip` and a `json.zip` produces
 * `<person>/html.zip.extracted/html/` and `<person>/json.zip.extracted/json/` — two directories of
 * which neither contains the other, so detection saw two unrelated dumps. Promoting the single
 * child of each wrapper turns that back into `<person>/html/` and `<person>/json/`, which is the
 * ordinary shape the detector already handles, with no knowledge of split dumps anywhere in it.
 *
 * Checked against the whole corpus: nearly every nested archive holds exactly one top-level
 * directory. The ones that hold two (`Архив.zip` with `html/` and `json/` side by side) are left
 * alone and still work, because their candidates nest inside the wrapper and group on containment.
 */
internal object ArchiveFlattening {

    /**
     * Promotes `wrapper/<only child directory>` to [wrapper]'s parent, returning where the content
     * ended up — [wrapper] itself when nothing could be moved.
     *
     * Nothing is moved when the wrapper holds more than one entry or holds a file rather than a
     * directory: those are the archive's own shape, not a wrapper level to undo.
     */
    fun flatten(wrapper: File): File {
        val parent = wrapper.parentFile ?: return wrapper
        val onlyChild = wrapper.listFiles().orEmpty().singleOrNull()?.takeIf { it.isDirectory } ?: return wrapper
        val target = File(parent, onlyChild.name)
        if (target.exists()) return resolveDuplicate(wrapper, onlyChild, target)
        return promote(wrapper, onlyChild, target)
    }

    /**
     * Settles a nested archive whose contents are already sitting beside it under the same name.
     *
     * Three real exports ship a dump twice — the folder unpacked *and* an archive of that same
     * folder next to it (`Профиль/` beside `Профиль.rar`). Keeping both makes one dump import as two
     * profiles, so one copy has to go, and the one to keep is whichever holds more files: a folder
     * left half-extracted by whoever built the archive should not shadow a complete archive of the
     * same thing, and vice versa.
     *
     * A name taken by a *file* rather than a directory is left alone entirely — that is a
     * coincidence, not a duplicate, and there is nothing safe to merge.
     */
    private fun resolveDuplicate(wrapper: File, onlyChild: File, target: File): File {
        if (!target.isDirectory) return wrapper
        if (fileCount(onlyChild) <= fileCount(target)) {
            wrapper.deleteRecursively()
            return target
        }
        if (!target.deleteRecursively()) return wrapper
        return promote(wrapper, onlyChild, target)
    }

    private fun promote(wrapper: File, onlyChild: File, target: File): File {
        if (!onlyChild.renameTo(target)) return wrapper
        wrapper.deleteRecursively()
        return target
    }

    private fun fileCount(directory: File): Int = directory.walkTopDown().count { it.isFile }
}
