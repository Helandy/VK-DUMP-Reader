package com.etozhesandy.redpanda.core.archive.extract

/**
 * What to do with an archive entry while unpacking.
 *
 * [Full] is the only policy an import ever uses. [SkipContent] exists for the detection matrix
 * tool, which needs the *shape* of a 20 GB corpus and none of its bytes: writing an empty file in
 * place of every photo and video turns the whole `exampl/` folder into something that unpacks in
 * minutes, while the HTML and JSON detection actually reads stay byte-for-byte real.
 */
internal sealed interface EntryPolicy {

    data object Full : EntryPolicy

    /** Writes an empty file instead of the entry's content whenever [skip] returns true. */
    data class SkipContent(val skip: (name: String, size: Long) -> Boolean) : EntryPolicy
}

/** Whether [name] of [size] bytes should be written empty under this policy. */
internal fun EntryPolicy.skips(name: String, size: Long): Boolean = when (this) {
    EntryPolicy.Full -> false
    is EntryPolicy.SkipContent -> skip(name, size)
}
