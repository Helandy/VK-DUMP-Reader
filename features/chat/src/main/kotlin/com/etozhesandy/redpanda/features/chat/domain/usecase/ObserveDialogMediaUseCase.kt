package com.etozhesandy.redpanda.features.chat.domain.usecase

import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.features.chat.domain.repository.ChatRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Streams every photo/video attached anywhere in one dialog, in original order. */
class ObserveDialogMediaUseCase @Inject constructor(
    private val repository: ChatRepository,
) {
    operator fun invoke(dialogId: String): Flow<List<Attachment>> = repository.observeMediaForDialog(dialogId)
}
