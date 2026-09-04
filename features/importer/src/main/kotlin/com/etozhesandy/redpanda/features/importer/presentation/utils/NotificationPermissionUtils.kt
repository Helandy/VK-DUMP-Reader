package com.etozhesandy.redpanda.features.importer.presentation.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * Asking again once the permission is granted (or on releases that grant it at install time) would
 * put an unexplained dialog in front of a user who has already said yes.
 */
internal fun Context.needsNotificationPermission(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
