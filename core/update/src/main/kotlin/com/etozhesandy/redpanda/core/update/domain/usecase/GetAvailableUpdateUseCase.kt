package com.etozhesandy.redpanda.core.update.domain.usecase

import com.etozhesandy.redpanda.core.update.domain.model.AppRelease
import com.etozhesandy.redpanda.core.update.domain.repository.AppVersionProvider
import com.etozhesandy.redpanda.core.update.domain.repository.UpdateRepository
import com.etozhesandy.redpanda.core.update.utils.isVersionNewer
import javax.inject.Inject

/**
 * Возвращает опубликованный релиз, если он новее установленной версии, иначе null —
 * то есть «есть ли о чём сообщать пользователю».
 */
class GetAvailableUpdateUseCase @Inject constructor(
    private val updateRepository: UpdateRepository,
    private val appVersionProvider: AppVersionProvider,
) {
    suspend operator fun invoke(): AppRelease? {
        val latest = updateRepository.latestRelease() ?: return null
        return latest.takeIf { isVersionNewer(appVersionProvider.currentVersion(), it.version) }
    }
}
