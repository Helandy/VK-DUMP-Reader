package com.etozhesandy.redpanda.core.common.files

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Owns the on-disk layout `filesDir/profiles/$id/...` shared by every imported profile. */
@Singleton
class ProfileDirectories @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun rootDir(): File = File(context.filesDir, "profiles").apply { mkdirs() }

    fun profileDir(profileId: String): File = File(rootDir(), profileId)

    fun rawDir(profileId: String): File = File(profileDir(profileId), "raw")

    fun deleteProfileDir(profileId: String): Boolean = profileDir(profileId).deleteRecursively()

    /**
     * Whether [file] is one of the imported files this app owns.
     *
     * Attachment paths are read verbatim out of the archive, so a crafted export can name any
     * path on the device — including this app's own database. Anything that leaves the import
     * tree is not media and must not be read on an archive's say-so.
     */
    fun isInsideProfiles(file: File): Boolean = file.isInside(rootDir())
}
