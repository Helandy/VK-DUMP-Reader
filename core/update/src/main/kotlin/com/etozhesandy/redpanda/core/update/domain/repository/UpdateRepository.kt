package com.etozhesandy.redpanda.core.update.domain.repository

import com.etozhesandy.redpanda.core.update.domain.model.AppRelease

interface UpdateRepository {

    /**
     * Последний опубликованный релиз или null, если релизов нет либо проверка не удалась.
     * Недоступная сеть — это «обновления не видно», а не ошибка: проверка обновлений
     * фоновая и ничего в приложении не блокирует.
     */
    suspend fun latestRelease(): AppRelease?
}
