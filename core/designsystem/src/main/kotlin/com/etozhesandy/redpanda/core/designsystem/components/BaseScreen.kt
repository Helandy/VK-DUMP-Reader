package com.etozhesandy.redpanda.core.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.res.stringResource
import com.etozhesandy.redpanda.core.designsystem.R

/**
 * The frame every screen in the app is built on: a top bar with a title and an optional back
 * arrow, over a full-size body that already has the scaffold's insets applied.
 *
 * Screens call this instead of [Scaffold] directly so that padding, insets and the back arrow are
 * decided once here rather than copied into each feature.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseScreen(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    BaseScreen(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { BackButton(onBack) },
                actions = actions,
            )
        },
        modifier = modifier,
        floatingActionButton = floatingActionButton,
        content = content,
    )
}

/**
 * [BaseScreen] for the screens whose top bar is more than a title — the chat, whose bar switches
 * between the dialog name and a search field.
 */
@Composable
fun BaseScreen(
    topBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        floatingActionButton = floatingActionButton,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), content = content)
    }
}

/** Draws nothing when there is nowhere to go back to, e.g. on the start destination. */
@Composable
internal fun BackButton(onBack: (() -> Unit)?, tint: Color = Color.Unspecified) {
    if (onBack == null) return
    IconButton(onClick = onBack) {
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = stringResource(R.string.action_back),
            tint = if (tint.isSpecified) tint else LocalContentColor.current,
        )
    }
}
