package com.etozhesandy.redpanda.core.update.data.di

import com.etozhesandy.redpanda.core.update.data.AppVersionProviderImpl
import com.etozhesandy.redpanda.core.update.data.repository.GitHubUpdateRepository
import com.etozhesandy.redpanda.core.update.domain.repository.AppVersionProvider
import com.etozhesandy.redpanda.core.update.domain.repository.UpdateRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
internal abstract class UpdateModule {

    @Binds
    @Singleton
    abstract fun bindUpdateRepository(impl: GitHubUpdateRepository): UpdateRepository

    @Binds
    @Singleton
    abstract fun bindAppVersionProvider(impl: AppVersionProviderImpl): AppVersionProvider

    companion object {

        /**
         * Клиент только для проверки обновлений: короткие таймауты, потому что ответа никто не
         * ждёт — экран профилей уже показан, и опоздавший ответ просто ничего не покажет.
         */
        @Provides
        @Singleton
        @UpdateHttpClient
        fun provideUpdateHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

        @Provides
        @Singleton
        @UpdateJson
        fun provideUpdateJson(): Json = Json { ignoreUnknownKeys = true }

        private const val TIMEOUT_SECONDS = 10L
    }
}
