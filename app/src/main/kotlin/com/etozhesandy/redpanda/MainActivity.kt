package com.etozhesandy.redpanda

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.etozhesandy.redpanda.core.designsystem.theme.RedPandaTheme
import com.etozhesandy.redpanda.core.navigation.AppNavHost
import com.etozhesandy.redpanda.core.navigation.NavRegistrar
import com.etozhesandy.redpanda.core.navigation.manager.NavigationManager
import com.etozhesandy.redpanda.features.lock.presentation.AppLockGate
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var navRegistrars: Set<@JvmSuppressWildcards NavRegistrar>

    @Inject
    lateinit var navigationManager: NavigationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RedPandaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppLockGate {
                        val navController = rememberNavController()
                        AppNavHost(
                            navController = navController,
                            navManager = navigationManager,
                            registrars = navRegistrars,
                        )
                    }
                }
            }
        }
    }
}
