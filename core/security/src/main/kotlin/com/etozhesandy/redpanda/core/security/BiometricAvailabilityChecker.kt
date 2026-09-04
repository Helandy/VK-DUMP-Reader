package com.etozhesandy.redpanda.core.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import com.etozhesandy.redpanda.core.security.model.BiometricAvailability
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Reports whether a fingerprint (or comparable biometric) can be used to unlock the app. */
@Singleton
class BiometricAvailabilityChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun availability(): BiometricAvailability =
        when (BiometricManager.from(context).canAuthenticate(ALLOWED_AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            -> BiometricAvailability.NO_HARDWARE
            else -> BiometricAvailability.UNAVAILABLE
        }

    companion object {
        /** Weak biometrics are accepted: the PIN, not the sensor, is the security floor here. */
        const val ALLOWED_AUTHENTICATORS = Authenticators.BIOMETRIC_STRONG or Authenticators.BIOMETRIC_WEAK
    }
}
