package com.etozhesandy.redpanda.features.profile.utils

/**
 * What a media folder is called on screen: the last segment of its archive-relative path.
 *
 * The full path is what identifies the folder everywhere else (it is what an attachment's
 * `sourceFolder` matches), so it is never shortened in place — only for display.
 */
fun folderDisplayName(path: String): String = path.substringAfterLast('/')
