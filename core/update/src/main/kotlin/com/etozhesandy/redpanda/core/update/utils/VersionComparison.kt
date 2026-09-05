package com.etozhesandy.redpanda.core.update.utils

/**
 * Сравнивает версии вида `1.2.3` по числовым сегментам: отсутствующий сегмент считается нулём,
 * поэтому `1.2` и `1.2.0` — одна и та же версия. Нечисловой сегмент тоже читается как ноль:
 * тег релиза придуман не приложением, и гадать по нему нечего.
 */
fun isVersionNewer(current: String, remote: String): Boolean {
    val cur = current.split(".").map { it.toIntOrNull() ?: 0 }
    val rem = remote.split(".").map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(cur.size, rem.size)) {
        val c = cur.getOrElse(i) { 0 }
        val r = rem.getOrElse(i) { 0 }
        if (r > c) return true
        if (r < c) return false
    }
    return false
}
