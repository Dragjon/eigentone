package io.github.dragjon.eigentone.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.dragjon.eigentone.data.Repo

/** Brings the EQ back after a restart or app update, if it was on. */
class StartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> Unit
            else -> return
        }
        val state = Repo.state.value
        if (!state.enabled || !state.startOnBoot) return
        try {
            AudioService.start(context)
        } catch (e: Exception) {
            // The app still starts the EQ when opened, so this only delays it.
            Log.w("EigentoneStart", "Android blocked the automatic start", e)
        }
    }
}
