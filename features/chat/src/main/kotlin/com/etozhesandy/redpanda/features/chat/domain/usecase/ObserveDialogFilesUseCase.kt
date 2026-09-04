package com.etozhesandy.redpanda.features.chat.domain.usecase

import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.features.chat.domain.repository.ChatRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Streams every document attachment in one dialog, in original order. */
class ObserveDialogFilesUseCase @Inject constructor(
    private val repository: ChatRepository,
) {
    operator fun invoke(dialogId: String): Flow<List<Attachment>> = repository.observeFilesForDialog(dialogId)
}
