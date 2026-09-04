package com.etozhesandy.redpanda.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * The frame for full-screen media viewers: black everywhere, no title, white controls.
 *
 * Kept apart from [BaseScreen] rather than parameterised into it — a viewer deliberately ignores
 * the theme so that nothing competes with the image, and that is a different kind of screen, not a
 * colour option.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImmersiveScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { BackButton(onBack, tint = Color.White) },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black).padding(padding),
            content = content,
        )
    }
}
