package com.etozhesandy.redpanda.core.model

/**
 * Progress snapshot emitted while a profile archive is being imported.
 *
 * [current]/[total] count saved messages; [dialogsDone]/[dialogsTotal] count dialogs, which is
 * what the user actually sees ("11 из 2200"). The dialog total is known only once the parser has
 * enumerated the archive, so it stays 0 for the copy/extract stages.
 */
data class ImportProgress(
    val stage: ImportStage,
    val current: Int = 0,
    val total: Int = 0,
    val message: String? = null,
    val dialogsDone: Int = 0,
    val dialogsTotal: Int = 0,
)
