package com.etozhesandy.redpanda.core.common.net

import java.net.URI

/**
 * The single place a string coming out of an imported archive is decided to be a safe URL, a
 * local file path, or neither.
 *
 * Everything an archive carries — attachment `href`s, `screen_name`, avatar `src` — is untrusted:
 * the archive may have been produced by anyone. A bare `startsWith("http")` is not a scheme check
 * (`http-foo:` passes it), and string-concatenating a name into `https://vk.com/$name` is not a
 * host check (`@evil.com/x` turns the prefix into userinfo and the real host becomes evil.com).
 * Both are parsed properly here instead, once, so every caller gets the same answer.
 *
 * Deliberately built on [URI] rather than `android.net.Uri`: it is the stricter parser of the two —
 * it rejects malformed input instead of guessing — and it keeps this object a plain JVM unit test.
 */
object UrlGuard {

    private val WEB_SCHEMES = setOf("http", "https")

    /** Hosts the in-app WebView is allowed to stay on. */
    private val VK_HOSTS = setOf("vk.com", "vk.ru", "vkontakte.ru")

    /** What VK itself accepts as a screen name; anything else cannot be one. */
    private val SCREEN_NAME = Regex("^[A-Za-z0-9_.]{1,64}$")

    private val NUMERIC_ID = Regex("^[0-9]{1,20}$")

    /** A well-formed `http`/`https` URL with a real host — the only kind safe to hand to an Intent. */
    fun isWebUrl(raw: String): Boolean {
        val uri = parse(raw) ?: return false
        return uri.scheme?.lowercase() in WEB_SCHEMES && !uri.host.isNullOrBlank()
    }

    /** [isWebUrl] narrowed to `https` on VK itself or one of its subdomains. */
    fun isVkUrl(raw: String): Boolean {
        val uri = parse(raw) ?: return false
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        val host = uri.host?.lowercase() ?: return false
        return host in VK_HOSTS || VK_HOSTS.any { host.endsWith(".$it") }
    }

    /**
     * A filesystem path rather than a URL: no scheme at all. This is what rejects `content://`,
     * `file://` and every custom scheme an archive could name, which would otherwise be handed to
     * a media player or a file read as if they were paths.
     */
    fun isLocalPath(raw: String): Boolean {
        if (raw.isBlank()) return false
        val uri = parse(raw) ?: return false
        return uri.scheme == null
    }

    /** `https://vk.com/<name>`, or null when [screenName] is not a name VK could have issued. */
    fun vkProfileUrl(screenName: String?): String? =
        screenName?.takeIf(SCREEN_NAME::matches)?.let { "https://vk.com/$it" }

    /** `https://vk.com/<prefix><id>` (e.g. `id42`, `club7`), or null when [id] is not numeric. */
    fun vkIdUrl(prefix: String, id: String?): String? =
        id?.takeIf(NUMERIC_ID::matches)?.let { "https://vk.com/$prefix$it" }

    /** Malformed input is not a URL of any kind, so a parse failure is an answer, not an error. */
    private fun parse(raw: String): URI? = runCatching { URI(raw.trim()) }.getOrNull()
}
