package com.etozhesandy.redpanda.features.settings.presentation.utils

import android.content.Context
import com.etozhesandy.redpanda.features.settings.R
import kotlin.math.ln
import kotlin.math.pow

private val UNIT_RES_IDS = listOf(
    R.string.size_kilobytes,
    R.string.size_megabytes,
    R.string.size_gigabytes,
    R.string.size_terabytes,
)

fun formatBytes(context: Context, bytes: Long): String {
    if (bytes < 1024) return context.getString(R.string.size_bytes, bytes)
    val exponent = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(1, UNIT_RES_IDS.size)
    val value = bytes / 1024.0.pow(exponent.toDouble())
    return context.getString(UNIT_RES_IDS[exponent - 1], "%.1f".format(value))
}
