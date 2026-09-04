package com.etozhesandy.redpanda.core.common.files

import java.io.File

/**
 * Whether this file really lives under [root].
 *
 * Every name used to build a path during an import comes from content the user never inspected —
 * archive entry names, and the display names a DocumentsProvider reports — so `..` segments have
 * to be assumed. Resolved through [File.canonicalFile] rather than [File.getAbsolutePath] so a
 * symlink written by the archive cannot point out of the directory it was extracted into.
 *
 * A file that is [root] itself is not "inside" it: callers use this to guard children.
 */
fun File.isInside(root: File): Boolean = runCatching {
    canonicalFile.path.startsWith(root.canonicalFile.path + File.separator)
}.getOrDefault(false)
