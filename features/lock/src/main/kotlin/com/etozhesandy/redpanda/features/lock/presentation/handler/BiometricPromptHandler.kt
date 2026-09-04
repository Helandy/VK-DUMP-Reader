package com.etozhesandy.redpanda.features.lock.presentation.handler

import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.etozhesandy.redpanda.core.security.BiometricAvailabilityChecker
import com.etozhesandy.redpanda.features.lock.R
import com.etozhesandy.redpanda.features.lock.presentation.utils.findActivity

/**
 * Builds a launcher for the system biometric dialog.
 *
 * The prompt is hosted by the activity, so it survives a rotation on its own; the composable only
 * translates its callbacks back into screen events. Returns a no-op launcher when the hosting
 * context is not a [FragmentActivity], which `androidx.biometric` requires.
 */
@Composable
fun rememberBiometricLauncher(
    onSucceeded: () -> Unit,
    onFailed: () -> Unit,
    onError: (Int) -> Unit,
): BiometricLauncher {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() as? FragmentActivity }
    val currentSucceeded = rememberUpdatedState(onSucceeded)
    val currentFailed = rememberUpdatedState(onFailed)
    val currentError = rememberUpdatedState(onError)

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(stringResource(R.string.lock_prompt_title))
        .setSubtitle(stringResource(R.string.lock_prompt_subtitle))
        // A negative button is required here and cannot be combined with DEVICE_CREDENTIAL; the
        // app's own PIN is the fallback, not the device's.
        .setNegativeButtonText(stringResource(R.string.lock_use_pin))
        .setAllowedAuthenticators(BiometricAvailabilityChecker.ALLOWED_AUTHENTICATORS)
        .build()

    val prompt = remember(activity) {
        activity?.let {
            BiometricPrompt(
                it,
                ContextCompat.getMainExecutor(it),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        currentSucceeded.value()
                    }

                    override fun onAuthenticationFailed() {
                        currentFailed.value()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        currentError.value(errorCode)
                    }
                },
            )
        }
    }

    return remember(prompt, promptInfo) {
        BiometricLauncher(
            launch = { prompt?.authenticate(promptInfo) },
            // The dialog does not close itself when the screen gives up on the fingerprint, so
            // falling back to the PIN has to take it down explicitly.
            cancel = { prompt?.cancelAuthentication() },
        )
    }
}
