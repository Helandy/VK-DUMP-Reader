package com.etozhesandy.redpanda.core.model

/** Lifecycle state of an imported [Profile]. */
enum class ProfileStatus {
    IMPORTING,
    READY,
    ERROR,
}
