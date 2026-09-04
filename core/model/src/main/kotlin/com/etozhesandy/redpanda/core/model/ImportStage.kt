package com.etozhesandy.redpanda.core.model

/** Steps of [ImportProgress] during a profile import. */
enum class ImportStage {
    COPYING,
    EXTRACTING,
    PARSING,
    SAVING,
    DONE,
    ERROR,
}
