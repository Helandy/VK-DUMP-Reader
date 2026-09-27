package com.etozhesandy.redpanda.core.common.logging

import android.content.Context
import android.os.Build
import android.os.Process
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLoggerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppLogger {

    private val logDir = File(context.filesDir, LOG_DIR)
    private val lock = Any()

    // Read from the crash handler, which runs on whichever thread failed.
    @Volatile
    private var enabled = false
    private var logcat: java.lang.Process? = null
    private var crashHandlerInstalled = false

    override fun setEnabled(enabled: Boolean) {
        synchronized(lock) {
            this.enabled = enabled
            if (enabled) {
                installCrashHandler()
                startLogcat()
            } else {
                stopLogcat()
            }
        }
    }

    override fun logFiles(): List<File> =
        logDir.listFiles()
            ?.filter { it.isFile && it.length() > 0 }
            ?.sortedBy { it.name }
            .orEmpty()

    override fun clear() {
        synchronized(lock) {
            stopLogcat()
            logDir.listFiles()?.forEach { it.delete() }
            if (enabled) startLogcat()
        }
    }

    private fun startLogcat() {
        if (logcat != null) return
        runCatching {
            logDir.mkdirs()
            val logFile = File(logDir, LOGCAT_FILE)
            logFile.appendText(header("Session started"))
            // An app may read its own process' log without any permission. Without `-T` logcat
            // first dumps what the buffer still holds for this pid, so lines written before the
            // setting was read are not lost. `-r`/`-n` keep at most two files of 1 MB each.
            logcat = ProcessBuilder(
                "logcat",
                "-v", "threadtime",
                "--pid=${Process.myPid()}",
                "-f", logFile.absolutePath,
                "-r", LOGCAT_ROTATE_KB.toString(),
                "-n", LOGCAT_ROTATE_COUNT.toString(),
            ).redirectErrorStream(true).start()
        }
    }

    private fun stopLogcat() {
        logcat?.destroy()
        logcat = null
    }

    private fun installCrashHandler() {
        if (crashHandlerInstalled) return
        crashHandlerInstalled = true
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Written synchronously and separately from the logcat file: the logcat child dies
            // with the app and may not get to flush the FATAL EXCEPTION lines.
            if (enabled) runCatching { writeCrash(thread, throwable) }
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun writeCrash(thread: Thread, throwable: Throwable) {
        logDir.mkdirs()
        val file = File(logDir, "$CRASH_PREFIX${fileTimestamp()}.txt")
        PrintWriter(file).use { writer ->
            writer.print(header("Crash in thread \"${thread.name}\""))
            throwable.printStackTrace(writer)
        }
        logDir.listFiles { f -> f.name.startsWith(CRASH_PREFIX) }
            ?.sortedByDescending { it.name }
            ?.drop(MAX_CRASH_FILES)
            ?.forEach { it.delete() }
    }

    private fun header(title: String): String = buildString {
        appendLine("===== $title: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())} =====")
        appendLine("App: ${appVersion()}")
        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        appendLine()
    }

    private fun appVersion(): String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull()
            ?: "?"

    private fun fileTimestamp(): String = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())

    private companion object {
        /** Must match the path exposed by the app's FileProvider in `res/xml/file_paths.xml`. */
        const val LOG_DIR = "logs"
        const val LOGCAT_FILE = "app.log"
        const val LOGCAT_ROTATE_KB = 1024
        const val LOGCAT_ROTATE_COUNT = 2
        const val CRASH_PREFIX = "crash-"
        const val MAX_CRASH_FILES = 5
    }
}
