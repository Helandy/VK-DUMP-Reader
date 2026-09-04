package com.etozhesandy.redpanda.features.dialogs.domain.usecase

import com.etozhesandy.redpanda.core.model.ChatDialog
import com.etozhesandy.redpanda.features.dialogs.domain.repository.DialogsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Streams a profile's dialogs, optionally filtered by peer name and source category. */
class ObserveDialogsUseCase @Inject constructor(
    private val repository: DialogsRepository,
) {
    operator fun invoke(profileId: String, query: String?, category: String?): Flow<List<ChatDialog>> =
        repository.observeDialogs(profileId, query?.takeIf { it.isNotBlank() }, category)
}
