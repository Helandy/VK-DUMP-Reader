package com.etozhesandy.redpanda.core.common.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlGuardTest {

    @Test
    fun `web url accepts http and https`() {
        assertTrue(UrlGuard.isWebUrl("https://vk.com/id1"))
        assertTrue(UrlGuard.isWebUrl("http://example.com/a.jpg"))
    }

    @Test
    fun `web url rejects schemes an archive could name`() {
        assertFalse(UrlGuard.isWebUrl("javascript:alert(1)"))
        assertFalse(UrlGuard.isWebUrl("intent://scan/#Intent;scheme=zxing;end"))
        assertFalse(UrlGuard.isWebUrl("content://com.other.app/secret"))
        assertFalse(UrlGuard.isWebUrl("file:///data/data/com.etozhesandy.redpanda/databases/redpanda.db"))
    }

    /** `startsWith("http")` — the check this replaced — passes this string. */
    @Test
    fun `web url rejects a scheme merely starting with http`() {
        assertFalse(UrlGuard.isWebUrl("http-foo:bar"))
    }

    @Test
    fun `web url rejects input that is not a url at all`() {
        assertFalse(UrlGuard.isWebUrl(""))
        assertFalse(UrlGuard.isWebUrl("https://"))
        assertFalse(UrlGuard.isWebUrl("http://a b c"))
    }

    @Test
    fun `vk url accepts vk and its subdomains`() {
        assertTrue(UrlGuard.isVkUrl("https://vk.com/id1"))
        assertTrue(UrlGuard.isVkUrl("https://m.vk.com/id1"))
        assertTrue(UrlGuard.isVkUrl("https://VK.COM/id1"))
    }

    /** The whole point of the guard: the prefix becomes userinfo and the real host is evil.com. */
    @Test
    fun `vk url rejects a userinfo host`() {
        assertFalse(UrlGuard.isVkUrl("https://vk.com@evil.com/x"))
    }

    @Test
    fun `vk url rejects lookalike hosts and plain http`() {
        assertFalse(UrlGuard.isVkUrl("https://evil-vk.com/x"))
        assertFalse(UrlGuard.isVkUrl("https://vk.com.evil.com/x"))
        assertFalse(UrlGuard.isVkUrl("http://vk.com/x"))
    }

    @Test
    fun `local path is a path with no scheme`() {
        assertTrue(UrlGuard.isLocalPath("/data/user/0/app/files/profiles/x/raw/photo.jpg"))
        assertTrue(UrlGuard.isLocalPath("photos/1.jpg"))
        assertFalse(UrlGuard.isLocalPath("https://vk.com/x"))
        assertFalse(UrlGuard.isLocalPath("content://com.other.app/secret"))
        assertFalse(UrlGuard.isLocalPath(""))
    }

    @Test
    fun `profile url is built only from a name vk could have issued`() {
        assertEquals("https://vk.com/durov", UrlGuard.vkProfileUrl("durov"))
        assertNull(UrlGuard.vkProfileUrl("@evil.com/x"))
        assertNull(UrlGuard.vkProfileUrl("a/../b"))
        assertNull(UrlGuard.vkProfileUrl(""))
        assertNull(UrlGuard.vkProfileUrl(null))
    }

    @Test
    fun `id url requires a numeric id`() {
        assertEquals("https://vk.com/id42", UrlGuard.vkIdUrl("id", "42"))
        assertEquals("https://vk.com/club7", UrlGuard.vkIdUrl("club", "7"))
        assertNull(UrlGuard.vkIdUrl("id", "42@evil.com"))
        assertNull(UrlGuard.vkIdUrl("id", "-42"))
        assertNull(UrlGuard.vkIdUrl("id", null))
    }
}
