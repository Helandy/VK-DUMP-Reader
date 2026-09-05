package com.etozhesandy.redpanda.core.update.data

import android.content.Context
import android.content.pm.PackageManager
import com.etozhesandy.redpanda.core.update.domain.repository.AppVersionProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Версия берётся из установленного пакета, а не из BuildConfig: так модулю обновлений не нужно
 * знать ни о конфигурации сборки, ни о модуле `:app`.
 */
@Singleton
internal class AppVersionProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppVersionProvider {

    /** Пустая строка, когда версию узнать не удалось: она старше любого релиза, и это верно. */
    override fun currentVersion(): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull().orEmpty()
}
