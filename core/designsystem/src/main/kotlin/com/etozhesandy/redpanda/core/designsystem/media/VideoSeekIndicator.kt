package com.etozhesandy.redpanda.core.designsystem.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.etozhesandy.redpanda.core.designsystem.R

/**
 * Brief confirmation of a double-tap seek over a [VideoPlayer], on the half of the video that was
 * tapped: back on the left, forward on the right.
 */
@Composable
fun VideoSeekIndicator(
    isForward: Boolean,
    stepSeconds: Int,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .align(if (isForward) Alignment.CenterEnd else Alignment.CenterStart)
                .padding(horizontal = 48.dp)
                .size(96.dp)
                .background(Color.Black.copy(alpha = 0.45f), CircleShape),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = if (isForward) Icons.Default.FastForward else Icons.Default.FastRewind,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = stringResource(
                    if (isForward) R.string.video_seek_forward else R.string.video_seek_back,
                    stepSeconds,
                ),
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
