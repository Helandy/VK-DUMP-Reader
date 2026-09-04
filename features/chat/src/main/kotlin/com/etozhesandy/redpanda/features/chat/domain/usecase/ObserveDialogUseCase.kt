package com.etozhesandy.redpanda.features.chat.domain.usecase

import com.etozhesandy.redpanda.core.model.ChatDialog
import com.etozhesandy.redpanda.features.chat.domain.repository.ChatRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Streams the dialog's own metadata (peer name/avatar) shown in the chat's top bar. */
class ObserveDialogUseCase @Inject constructor(
    private val repository: ChatRepository,
) {
    operator fun invoke(dialogId: String): Flow<ChatDialog?> = repository.observeDialog(dialogId)
}
