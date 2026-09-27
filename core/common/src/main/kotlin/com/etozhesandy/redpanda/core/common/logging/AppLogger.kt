package com.etozhesandy.redpanda.core.common.logging

import java.io.File

/**
 * Opt-in diagnostic log: while enabled, the app's own logcat output and the stack trace of any
 * crash are written to files in internal storage, so the user can send them with a bug report.
 */
interface AppLogger {

    /** Starts or stops recording. Safe to call repeatedly with the same value. */
    fun setEnabled(enabled: Boolean)

    /** Non-empty log files, oldest name first. */
    fun logFiles(): List<File>

    /** Deletes every recorded file; recording continues into a fresh file if it is enabled. */
    fun clear()
}
