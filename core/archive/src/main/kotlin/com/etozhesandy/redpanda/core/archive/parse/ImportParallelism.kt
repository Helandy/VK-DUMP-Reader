package com.etozhesandy.redpanda.core.archive.parse

/**
 * How many dialogs a parser may work on at once. Each one holds a whole page in memory (a Jsoup
 * DOM or a JSON tree, several times the file's size), so the count follows the heap the process
 * actually has rather than a fixed number: four parallel parses on a 256 MB heap, alongside a
 * media viewer decoding photos, ran the app out of memory.
 */
internal fun importParallelism(): Int =
    (Runtime.getRuntime().maxMemory() / HEAP_PER_PARSE_BYTES).toInt().coerceIn(1, MAX_PARALLELISM)

private const val HEAP_PER_PARSE_BYTES = 128L * 1024 * 1024
private const val MAX_PARALLELISM = 4
