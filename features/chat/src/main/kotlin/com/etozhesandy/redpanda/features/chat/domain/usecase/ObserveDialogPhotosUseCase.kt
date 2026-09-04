package com.etozhesandy.redpanda.features.chat.domain.usecase

import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.features.chat.domain.repository.ChatRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Streams every photo attached anywhere in one dialog, in original order. */
class ObserveDialogPhotosUseCase @Inject constructor(
    private val repository: ChatRepository,
) {
    operator fun invoke(dialogId: String): Flow<List<Attachment>> = repository.observePhotosForDialog(dialogId)
}
