package com.etozhesandy.redpanda.features.profile.presentation.profile.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.etozhesandy.redpanda.core.model.Group
import com.etozhesandy.redpanda.features.profile.R

@Composable
fun GroupsPreviewSection(
    groups: List<Group>,
    count: Int,
    onAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProfilePreviewSection(
        title = stringResource(R.string.profile_groups),
        count = count,
        onAllClick = onAllClick,
        modifier = modifier,
    ) {
        items(groups, key = { it.id }) { group ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.size(64.dp)) {
                CircleAvatar(group.avatarPath, size = 56.dp)
            }
        }
    }
}
