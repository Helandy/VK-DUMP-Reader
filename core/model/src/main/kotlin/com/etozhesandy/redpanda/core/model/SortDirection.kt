package com.etozhesandy.redpanda.core.model

/**
 * The rule every sort menu in the app follows: re-picking the key that is already active flips the
 * direction, while picking a different key starts from that key's own [naturalAscending].
 *
 * [current] is the *effective* sort, which may still be the one coming from the settings rather
 * than a previous pick on the same screen — so the first tap on the active key flips it too.
 */
fun <T> nextAscending(picked: T, current: T, currentAscending: Boolean, natural: Boolean): Boolean =
    if (picked == current) !currentAscending else natural
