package com.etozhesandy.redpanda.features.lock.presentation

import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.etozhesandy.redpanda.core.security.model.LockState as AppLockState
import com.etozhesandy.redpanda.features.lock.presentation.utils.findActivity

/**
 * Wraps the app content and replaces it with the lock screen whenever the app is locked. It sits
 * above the nav host rather than inside it, so the back stack is untouched by locking and there is
 * no destination a deep link could use to skip the gate.
 */
@Composable
fun AppLockGate(content: @Composable () -> Unit) {
    val viewModel: AppLockGateViewModel = hiltViewModel()
    val lockState by viewModel.lockState.collectAsStateWithLifecycle()

    when (lockState) {
        // Show nothing rather than flashing the content before the stored config has been read.
        AppLockState.Unknown -> Box(modifier = Modifier.fillMaxSize())
        AppLockState.Locked -> {
            SecureWindow()
            val lockViewModel: LockViewModel = hiltViewModel()
            val state by lockViewModel.state.collectAsStateWithLifecycle()
            LockScreen(state = state, onEvent = lockViewModel::onEvent)
        }
        else -> content()
    }
}

/** Keeps the locked app out of the recents preview and off screenshots while it is locked. */
@Composable
private fun SecureWindow() {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    DisposableEffect(activity) {
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
}
