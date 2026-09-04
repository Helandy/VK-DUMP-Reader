package com.etozhesandy.redpanda.features.profile.presentation.webview

import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.etozhesandy.redpanda.core.common.net.UrlGuard
import com.etozhesandy.redpanda.core.common.net.openExternally
import com.etozhesandy.redpanda.core.designsystem.components.BaseScreen
import com.etozhesandy.redpanda.core.designsystem.components.EmptyState
import com.etozhesandy.redpanda.features.profile.R

/**
 * Shows a VK page in-app, and only a VK page.
 *
 * [url] is built from archive content (a `screen_name` the export chose), so it is checked here
 * rather than trusted: `https://vk.com` prefixed onto `@evil.com/x` is a URL whose real host is
 * evil.com, and JavaScript is on. Every later navigation is checked the same way — a redirect is
 * as untrusted as the first load — and anything off VK is handed to the browser, which is the
 * right place for it and is sandboxed away from this app.
 */
@Composable
fun WebViewScreen(
    url: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }

    BackHandler {
        val view = webView
        if (view != null && view.canGoBack()) view.goBack() else onBack()
    }

    BaseScreen(title = url, modifier = modifier, onBack = onBack) {
        if (!UrlGuard.isVkUrl(url)) {
            EmptyState(text = stringResource(R.string.profile_link_blocked))
            return@BaseScreen
        }
        AndroidView(
            factory = { viewContext ->
                WebView(viewContext).apply {
                    settings.javaScriptEnabled = true
                    // The page is remote and untrusted; it has no business reaching the app's own
                    // files or content providers. `allowFileAccess` in particular defaults to true
                    // below API 30, which is inside this app's minSdk range.
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    @Suppress("DEPRECATION")
                    settings.allowFileAccessFromFileURLs = false
                    @Suppress("DEPRECATION")
                    settings.allowUniversalAccessFromFileURLs = false
                    settings.setGeolocationEnabled(false)
                    settings.javaScriptCanOpenWindowsAutomatically = false
                    webViewClient = VkOnlyWebViewClient { external -> context.openExternally(external) }
                    loadUrl(url)
                    webView = this
                }
            },
        )
    }
}

/** Keeps the WebView on VK: everything else is passed to [onExternalUrl] and never loaded here. */
private class VkOnlyWebViewClient(
    private val onExternalUrl: (String) -> Unit,
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val target = request?.url?.toString() ?: return true
        if (UrlGuard.isVkUrl(target)) return false
        onExternalUrl(target)
        return true
    }

    /**
     * A server-side redirect can land the view on another host without ever going through
     * [shouldOverrideUrlLoading]; stopping here means such a page never runs its scripts.
     */
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        // BLANK_PAGE is excluded because it is what this very branch navigates to: without that
        // the callback would fire again for it and loop.
        if (url == null || url == BLANK_PAGE || UrlGuard.isVkUrl(url)) return
        view?.stopLoading()
        view?.loadUrl(BLANK_PAGE)
        onExternalUrl(url)
    }

    private companion object {
        const val BLANK_PAGE = "about:blank"
    }
}
