package com.etozhesandy.redpanda.core.security.di

import javax.inject.Qualifier

/** Distinguishes the app-lock DataStore from the general settings one, which has the same type. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppLockDataStore
