package com.etozhesandy.redpanda.features.chat.data

import android.content.Context
import android.media.MediaMetadataRetriever
import com.etozhesandy.redpanda.core.common.dispatcher.IoDispatcher
import com.etozhesandy.redpanda.features.chat.domain.model.PreparedAudio
import com.etozhesandy.redpanda.features.chat.domain.repository.AudioRepository
import com.etozhesandy.redpanda.features.chat.utils.estimateMp3DurationMs
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * Keeps a local copy of each linked audio in the app's cache and works out its length from it.
 *
 * VK's CDN answers a voice-message link with the whole file, chunked, no `Content-Length` and no
 * range support — so neither its length nor a seek position can be had without the whole file,
 * which for a voice message is some tens of kilobytes. Results are kept for the process, failures
 * included, so scrolling back past a dead link doesn't knock on it again.
 */
@Singleton
class AudioRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AudioRepository {

    private val prepared = ConcurrentHashMap<String, Result<PreparedAudio>>()

    /** A screen of rows asks at once; a few downloads at a time keep the first ones from waiting on the last. */
    private val permits = Semaphore(MAX_CONCURRENT_DOWNLOADS)

    private val cacheDir: File by lazy { File(context.cacheDir, CACHE_DIR_NAME).apply { mkdirs() } }

    override suspend fun prepare(path: String): PreparedAudio? {
        prepared[path]?.let { return it.getOrNull() }
        return withContext(ioDispatcher) {
            permits.withPermit {
                prepared.getOrPut(path) { runCatching { prepareNow(path) } }.getOrNull()
            }
        }
    }

    private fun prepareNow(path: String): PreparedAudio {
        val file = if (path.startsWith("http")) download(path) else File(path)
        return PreparedAudio(localPath = file.absolutePath, durationMs = durationOf(file))
    }

    private fun download(url: String): File {
        val target = File(cacheDir, sha1(url) + "." + url.substringAfterLast('.').substringBefore('?').take(4))
        if (target.length() > 0) return target

        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            check(connection.responseCode == HttpURLConnection.HTTP_OK) { "HTTP ${connection.responseCode} for $url" }
            // Written aside and renamed, so a download cut short is never mistaken for the file.
            val partial = File(cacheDir, target.name + ".part")
            connection.inputStream.use { input -> partial.outputStream().use { input.copyTo(it) } }
            check(partial.renameTo(target)) { "Could not store $url" }
            return target
        } finally {
            connection.disconnect()
        }
    }

    private fun durationOf(file: File): Long? {
        val head = file.inputStream().use { input -> ByteArray(HEAD_BYTES).let { it.copyOf(input.read(it).coerceAtLeast(0)) } }
        return estimateMp3DurationMs(head, file.length()) ?: retrieverDuration(file)
    }

    private fun retrieverDuration(file: File): Long? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.takeIf { it > 0 }
        } catch (_: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    private fun sha1(value: String): String =
        MessageDigest.getInstance("SHA-1").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

    private companion object {
        const val CACHE_DIR_NAME = "audio"
        const val MAX_CONCURRENT_DOWNLOADS = 3
        const val HEAD_BYTES = 16 * 1024
        const val TIMEOUT_MS = 15_000
    }
}
