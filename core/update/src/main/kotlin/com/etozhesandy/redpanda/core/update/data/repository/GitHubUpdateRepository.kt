package com.etozhesandy.redpanda.core.update.data.repository

import com.etozhesandy.redpanda.core.common.dispatcher.IoDispatcher
import com.etozhesandy.redpanda.core.update.data.UpdateConfig
import com.etozhesandy.redpanda.core.update.data.di.UpdateHttpClient
import com.etozhesandy.redpanda.core.update.data.di.UpdateJson
import com.etozhesandy.redpanda.core.update.data.dto.GitHubReleaseDto
import com.etozhesandy.redpanda.core.update.data.mapper.toDomain
import com.etozhesandy.redpanda.core.update.domain.model.AppRelease
import com.etozhesandy.redpanda.core.update.domain.repository.UpdateRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Последний релиз из GitHub Releases. Запрос намеренно анонимный: api.github.com — публичный
 * сторонний сервис, и ничего своего приложению ему отправлять незачем.
 */
internal class GitHubUpdateRepository @Inject constructor(
    @param:UpdateHttpClient private val httpClient: OkHttpClient,
    @param:UpdateJson private val json: Json,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UpdateRepository {

    override suspend fun latestRelease(): AppRelease? = withContext(ioDispatcher) {
        runCatching {
            val request = Request.Builder()
                .url(LATEST_RELEASE_URL)
                .header("Accept", GITHUB_ACCEPT)
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                json.decodeFromString<GitHubReleaseDto>(response.body.string()).toDomain()
            }
        }.getOrNull()
    }

    private companion object {
        const val GITHUB_ACCEPT = "application/vnd.github+json"
        val LATEST_RELEASE_URL =
            "https://api.github.com/repos/" +
                "${UpdateConfig.GITHUB_OWNER}/${UpdateConfig.GITHUB_REPO}/releases/latest"
    }
}
