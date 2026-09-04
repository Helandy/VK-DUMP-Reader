package com.etozhesandy.redpanda.core.common.net

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Hands [url] to whatever app claims it; silently does nothing when none does, or when [url] is
 * not a plain web address.
 *
 * The scheme check is the point: these URLs come from imported archives, and `ACTION_VIEW` on an
 * archive-chosen scheme is a way to reach another app's deep link, a `content://` provider or a
 * `file://` path rather than a web page.
 */
fun Context.openExternally(url: String) {
    if (!UrlGuard.isWebUrl(url)) return
    runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
