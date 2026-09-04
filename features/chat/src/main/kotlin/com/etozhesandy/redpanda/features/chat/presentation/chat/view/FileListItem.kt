package com.etozhesandy.redpanda.features.chat.presentation.chat.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.etozhesandy.redpanda.core.designsystem.media.label
import com.etozhesandy.redpanda.core.model.Attachment

/**
 * A document from a dialog. No export downloads these — they stay behind a `vk.com/doc…` link — so
 * the row opens externally, and one without a link is shown but not clickable.
 */
@Composable
fun FileListItem(
    attachment: Attachment,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isOpenable = attachment.path.isNotBlank()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isOpenable) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 12.dp),
        )
        Text(
            text = attachment.caption?.takeIf { it.isNotBlank() } ?: attachment.type.label(),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (isOpenable) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}
