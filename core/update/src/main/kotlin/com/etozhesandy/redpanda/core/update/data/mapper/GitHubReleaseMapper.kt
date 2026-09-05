package com.etozhesandy.redpanda.core.update.data.mapper

import com.etozhesandy.redpanda.core.common.net.UrlGuard
import com.etozhesandy.redpanda.core.update.data.dto.GitHubReleaseDto
import com.etozhesandy.redpanda.core.update.domain.model.AppRelease

/**
 * Черновик и предрелиз — ещё не выпущенные версии, а адрес приходит со стороннего сервиса и
 * попадёт в `ACTION_VIEW`, поэтому непроверенный адрес обновлением не считается: и то, и другое
 * маппится в null, что для вызывающего значит «релиза нет».
 */
internal fun GitHubReleaseDto.toDomain(): AppRelease? {
    if (draft || prerelease) return null
    if (!UrlGuard.isWebUrl(htmlUrl)) return null
    val version = tagName.trim().trimStart('v', 'V')
    if (version.isBlank()) return null
    return AppRelease(version = version, releaseUrl = htmlUrl)
}
