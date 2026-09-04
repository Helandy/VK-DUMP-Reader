package com.etozhesandy.redpanda.core.common.mvi

import androidx.lifecycle.SavedStateHandle

/**
 * Enums stored in a [SavedStateHandle] go in by name, because process death has to be able to
 * restore them from a Bundle. Reading one back has to survive the enum having lost that constant
 * since — an app update, say — which is why an unknown name is null rather than an exception.
 */
inline fun <reified T : Enum<T>> SavedStateHandle.getEnum(key: String): T? =
    get<String>(key)?.let { name -> enumValues<T>().firstOrNull { it.name == name } }

fun <T : Enum<T>> SavedStateHandle.putEnum(key: String, value: T) {
    set(key, value.name)
}
