package io.github.dragjon.eigentone.data

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Keeps the most recent crash on the phone. Nothing is ever sent automatically: the user chooses
 * whether to share it from the System tab.
 */
object CrashLog {
    private const val FILE = "last-crash.txt"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
                file(appContext).writeText("${deviceSummary(appContext)}\n\n$trace")
            }
            previous?.uncaughtException(thread, error)
        }
    }

    fun read(context: Context): String? = file(context).takeIf { it.exists() }?.readText()

    fun clear(context: Context) {
        file(context).delete()
    }

    fun deviceSummary(context: Context): String {
        val version = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        return "Eigentone $version on ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}"
    }

    private fun file(context: Context) = File(context.filesDir, FILE)
}
