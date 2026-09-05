package com.etozhesandy.redpanda.core.update.data.di

import javax.inject.Qualifier

/**
 * Клиент и парсер здесь настроены под одну задачу — короткий анонимный запрос к api.github.com, —
 * поэтому они помечены, а не отданы приложению как общие HTTP-зависимости.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class UpdateHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class UpdateJson
