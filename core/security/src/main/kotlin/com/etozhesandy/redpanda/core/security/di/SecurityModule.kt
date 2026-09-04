package com.etozhesandy.redpanda.core.security.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.appLockDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_lock")

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    @AppLockDataStore
    fun provideAppLockDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.appLockDataStore
}
