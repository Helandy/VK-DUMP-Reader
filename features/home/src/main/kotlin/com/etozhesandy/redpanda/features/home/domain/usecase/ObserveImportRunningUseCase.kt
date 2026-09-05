package com.etozhesandy.redpanda.features.home.domain.usecase

import com.etozhesandy.redpanda.features.home.domain.repository.HomeRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Whether an import is enqueued or running, so the screen can keep a second one from starting. */
class ObserveImportRunningUseCase @Inject constructor(
    private val repository: HomeRepository,
) {
    operator fun invoke(): Flow<Boolean> = repository.observeImportRunning()
}
