package com.etozhesandy.redpanda.features.lock.presentation.handler

/** Shows and dismisses the system biometric dialog on behalf of the lock screen. */
class BiometricLauncher(
    val launch: () -> Unit,
    val cancel: () -> Unit,
)
