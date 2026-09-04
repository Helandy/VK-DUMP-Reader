package com.etozhesandy.redpanda.features.lock.presentation.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** Unwraps the composition's context to the hosting activity, which Compose does not hand out. */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
