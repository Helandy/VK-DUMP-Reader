package com.etozhesandy.redpanda.features.home.presentation.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.etozhesandy.redpanda.core.model.Profile
import com.etozhesandy.redpanda.core.model.ProfileStatus
import com.etozhesandy.redpanda.core.model.SourceType
import com.etozhesandy.redpanda.features.home.R

@Composable
fun ProfileListItem(
    profile: Profile,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileAvatar(profile)

        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f),
        ) {
            Text(text = profile.displayName, style = MaterialTheme.typography.titleMedium)
            Text(
                text = sourceWithStatus(profile.sourceType, profile.status),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        IconButton(onClick = onDeleteClick) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.profile_action_delete))
        }
    }
}

@Composable
private fun RowScope.ProfileAvatar(profile: Profile) {
    if (profile.avatarPath != null) {
        AsyncImage(
            model = profile.avatarPath,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(48.dp).clip(CircleShape),
        )
    } else {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(8.dp),
        )
    }
}

/** A ready profile reads as just its source; an unfinished one appends why it isn't usable yet. */
@Composable
private fun sourceWithStatus(sourceType: SourceType, status: ProfileStatus): String {
    val source = sourceLabel(sourceType)
    val statusRes = when (status) {
        ProfileStatus.IMPORTING -> R.string.profile_status_importing
        ProfileStatus.ERROR -> R.string.profile_status_error
        ProfileStatus.READY -> return source
    }
    return stringResource(R.string.profile_source_with_status, source, stringResource(statusRes))
}

/** The source name is a brand, so it is the same in every language. */
private fun sourceLabel(sourceType: SourceType): String = when (sourceType) {
    SourceType.VK -> "VK"
}
