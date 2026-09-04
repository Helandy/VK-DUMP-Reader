package com.etozhesandy.redpanda.features.profile.presentation.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group as GroupIcon
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.etozhesandy.redpanda.core.common.net.openExternally
import com.etozhesandy.redpanda.core.designsystem.components.BaseScreen
import com.etozhesandy.redpanda.core.designsystem.components.LoadableContent
import com.etozhesandy.redpanda.core.designsystem.components.SearchField
import com.etozhesandy.redpanda.core.model.Group
import com.etozhesandy.redpanda.features.profile.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest


@Composable
fun ProfileGroupsScreen(
    state: ProfileGroupsState.State,
    effect: Flow<ProfileGroupsState.Effect>,
    onEvent: (ProfileGroupsState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        effect.collectLatest { current ->
            when (current) {
                is ProfileGroupsState.Effect.OpenLink -> context.openExternally(current.url)
            }
        }
    }

    BaseScreen(
        title = stringResource(R.string.profile_groups),
        modifier = modifier,
        onBack = { onEvent(ProfileGroupsState.Event.BackClicked) },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SearchField(
                value = state.query,
                onValueChange = { onEvent(ProfileGroupsState.Event.QueryChanged(it)) },
                placeholder = stringResource(R.string.profile_search_by_name),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Box(modifier = Modifier.fillMaxSize()) {
                LoadableContent(
                    isLoading = state.isLoading,
                    isEmpty = state.groups.isEmpty(),
                    emptyText = stringResource(R.string.profile_empty_groups),
                ) {
                    LazyColumn {
                        items(state.groups, key = { it.id }) { group ->
                            GroupRow(group, onClick = { onEvent(ProfileGroupsState.Event.GroupClicked(group)) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupRow(group: Group, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (group.avatarPath != null) {
            AsyncImage(
                model = group.avatarPath,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(48.dp).clip(CircleShape),
            )
        } else {
            Icon(
                imageVector = Icons.Default.GroupIcon,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
            )
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(group.name, style = MaterialTheme.typography.titleMedium)
        }
    }
}
