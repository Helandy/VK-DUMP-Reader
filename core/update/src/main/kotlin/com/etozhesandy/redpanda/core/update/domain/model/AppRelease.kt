package com.etozhesandy.redpanda.core.update.domain.model

/**
 * Опубликованный релиз приложения: версия без префикса `v` и ссылка на страницу релиза
 * на GitHub, куда уходит пользователь по нажатию на плашку.
 */
data class AppRelease(
    val version: String,
    val releaseUrl: String,
)
