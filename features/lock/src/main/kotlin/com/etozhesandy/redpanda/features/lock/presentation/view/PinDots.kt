package com.etozhesandy.redpanda.features.lock.presentation.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** The row of dots showing how many digits of the PIN have been entered so far. */
@Composable
fun PinDots(
    filled: Int,
    length: Int,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    val activeColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(length) { index ->
            val isFilled = index < filled
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .border(width = 1.dp, color = activeColor, shape = CircleShape)
                    .background(
                        color = if (isFilled) activeColor else Color.Transparent,
                        shape = CircleShape,
                    ),
            )
        }
    }
}
