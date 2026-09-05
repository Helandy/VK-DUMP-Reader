package com.etozhesandy.redpanda.core.update.domain.repository

interface AppVersionProvider {

    /** Версия установленного приложения, с которой сравнивается опубликованный релиз. */
    fun currentVersion(): String
}
