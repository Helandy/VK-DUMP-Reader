package com.etozhesandy.redpanda.features.chat.domain.usecase

import com.etozhesandy.redpanda.features.chat.domain.repository.ChatRepository
import javax.inject.Inject

/** Marks or unmarks a message as favorite. */
class ToggleFavoriteUseCase @Inject constructor(
    private val repository: ChatRepository,
) {
    suspend operator fun invoke(messageId: String, isFavorite: Boolean) = repository.setFavorite(messageId, isFavorite)
}
