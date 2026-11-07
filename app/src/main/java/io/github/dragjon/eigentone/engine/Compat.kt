package io.github.dragjon.eigentone.engine

import android.app.ForegroundServiceStartNotAllowedException
import android.os.Build
import java.io.InputStream

/** Android 12 and newer can refuse to start a foreground service from the background. Android 11 never does. */
fun Throwable.isBackgroundStartRefused(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && this is ForegroundServiceStartNotAllowedException

/** Reads at most [limit] bytes. InputStream.readNBytes does this, but only from Android 13. */
fun InputStream.readAtMost(limit: Int): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (out.size() < limit) {
        val n = read(buffer, 0, minOf(buffer.size, limit - out.size()))
        if (n < 0) break
        out.write(buffer, 0, n)
    }
    return out.toByteArray()
}
