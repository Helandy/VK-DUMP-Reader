package com.etozhesandy.redpanda.core.common.files

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.etozhesandy.redpanda.core.common.R
import com.etozhesandy.redpanda.core.common.dispatcher.IoDispatcher
import com.etozhesandy.redpanda.core.common.net.UrlGuard
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Saves media into the public Downloads folder, under `Download/RedPanda/…`.
 *
 * The source is either a remote `http(s)` URL or a local path inside the profile's archive
 * directory — attachments come in both flavours (see `Attachment.path`).
 */
@Singleton
class ImageDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileDirectories: ProfileDirectories,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    /**
     * [folders] are human-readable names (profile, dialog, archive folder) nested under
     * `Download/RedPanda` in the order given. Each is sanitised into a safe folder name — nulls,
     * blanks and characters a filesystem won't take are dropped — and a name that itself contains
     * `/` (an archive's folder path) becomes one folder per segment.
     *
     * Returns the folder the file landed in, relative to the public Downloads folder, so the
     * caller can name it when reporting success.
     */
    suspend fun download(source: String, vararg folders: String?): Result<String> = withContext(ioDispatcher) {
        runCatching {
            val fileName = fileNameFor(source)
            val subPath = folders.filterNotNull()
                .flatMap { it.split('/', '\\') }
                .mapNotNull(::sanitizeFolderName)
                .joinToString("/")
            val relativePath = listOf("RedPanda", subPath).filter { it.isNotEmpty() }.joinToString("/")
            openSource(source).use { input ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, mimeTypeFor(fileName))
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$relativePath")
                    }
                    val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                        ?: error(context.getString(R.string.download_create_file_failed))
                    val output = context.contentResolver.openOutputStream(uri)
                        ?: error(context.getString(R.string.download_open_file_failed))
                    output.use { input.copyTo(it) }
                } else {
                    @Suppress("DEPRECATION")
                    val dir = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        relativePath,
                    )
                    dir.mkdirs()
                    FileOutputStream(File(dir, fileName)).use { input.copyTo(it) }
                }
                relativePath
            }
        }
    }

    /**
     * The destination is the *public* Downloads folder, so what this reads leaves the app sandbox.
     * A local path therefore has to be proven to be imported media before it is opened: the paths
     * come from the archive, and an archive that names `…/databases/redpanda.db` would otherwise
     * have this copy the user's whole message history somewhere every other app can read it.
     */
    private fun openSource(source: String): InputStream = when {
        UrlGuard.isWebUrl(source) -> URL(source).openStream()
        else -> {
            val path = source.removePrefix("file://")
            val file = File(path)
            require(UrlGuard.isLocalPath(path) && profileDirectories.isInsideProfiles(file)) {
                context.getString(R.string.download_source_outside_archive)
            }
            file.inputStream()
        }
    }

    /** Null when nothing usable is left — the caller then skips the folder level entirely. */
    private fun sanitizeFolderName(raw: String): String? = raw
        .map { if (it.isISOControl() || it in ILLEGAL_NAME_CHARS) '_' else it }
        .joinToString("")
        .trim()
        .trim('.')
        .take(MAX_FOLDER_NAME_LENGTH)
        .trim()
        .ifBlank { null }

    private fun fileNameFor(source: String): String {
        val raw = source.substringAfterLast('/').substringBefore('?')
        val name = raw.ifBlank { "media_${System.currentTimeMillis()}" }
        return if ('.' in name) name else "$name.jpg"
    }

    private fun mimeTypeFor(fileName: String): String = when (fileName.substringAfterLast('.', "").lowercase()) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        "mp4", "m4v" -> "video/mp4"
        "mov" -> "video/quicktime"
        "webm" -> "video/webm"
        "3gp" -> "video/3gpp"
        "mkv" -> "video/x-matroska"
        "mp3" -> "audio/mpeg"
        "ogg" -> "audio/ogg"
        "m4a" -> "audio/mp4"
        "wav" -> "audio/wav"
        else -> "image/jpeg"
    }

    private companion object {
        val ILLEGAL_NAME_CHARS = charArrayOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
        const val MAX_FOLDER_NAME_LENGTH = 60
    }
}
