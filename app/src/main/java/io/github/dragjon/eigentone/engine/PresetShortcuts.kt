package io.github.dragjon.eigentone.engine

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.util.Log
import io.github.dragjon.eigentone.R
import io.github.dragjon.eigentone.data.Preset
import io.github.dragjon.eigentone.ui.MainActivity

/** The presets offered when holding the app icon. */
object PresetShortcuts {
    const val ACTION_LOAD_PRESET = "io.github.dragjon.eigentone.LOAD_PRESET"
    const val EXTRA_PRESET_ID = "preset"
    private const val MAX_SHORT_LABEL = 25

    fun update(context: Context, presets: List<Preset>) {
        val manager = context.getSystemService(ShortcutManager::class.java) ?: return
        val limit = manager.maxShortcutCountPerActivity
        val shortcuts = presets.take(limit).map { preset ->
            ShortcutInfo.Builder(context, "preset:${preset.id}")
                .setShortLabel(preset.name.take(MAX_SHORT_LABEL))
                .setLongLabel(preset.name)
                .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
                .setIntent(
                    Intent(context, MainActivity::class.java)
                        .setAction(ACTION_LOAD_PRESET)
                        .putExtra(EXTRA_PRESET_ID, preset.id),
                )
                .build()
        }
        // Android limits how often these can change, which should never stop the app working.
        runCatching { manager.dynamicShortcuts = shortcuts }
            .onFailure { Log.w("EigentoneShortcuts", "Could not update shortcuts", it) }
    }
}
