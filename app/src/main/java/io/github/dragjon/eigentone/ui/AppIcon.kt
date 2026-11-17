package io.github.dragjon.eigentone.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import io.github.dragjon.eigentone.R
import io.github.dragjon.eigentone.engine.PresetShortcuts
import kotlin.math.abs

/**
 * Puts the app's home screen icon in the colour picked in Settings (user decision). Android can't recolour an
 * app's icon, so the manifest has one launcher entry per swatch colour, each with its own icon, and only the one
 * nearest the picked colour is left on. A colour of one's own, or the wallpaper's, takes the nearest swatch.
 */
object AppIcon {
    /** The launcher entries, in the order of [SeedScheme.SWATCHES]. */
    private val ALIASES = listOf("Blue", "Teal", "Green", "Amber", "Orange", "Red", "Pink", "Purple")

    /** Below this saturation a picked colour reads as grey, and keeps the blue icon. */
    private const val GREY_SATURATION = 0.15f

    /** The icons, in the same order. */
    private val ICONS = listOf(
        R.mipmap.ic_launcher, R.mipmap.ic_launcher_teal, R.mipmap.ic_launcher_green, R.mipmap.ic_launcher_amber,
        R.mipmap.ic_launcher_orange, R.mipmap.ic_launcher_red, R.mipmap.ic_launcher_pink, R.mipmap.ic_launcher_purple,
    )

    private fun component(context: Context, name: String) =
        ComponentName(context.packageName, "${context.packageName}.ui.Icon$name")

    private fun isOn(context: Context, name: String): Boolean =
        when (context.packageManager.getComponentEnabledSetting(component(context, name))) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> name == ALIASES.first()
            else -> false
        }

    private fun current(context: Context): Int = ALIASES.indexOfFirst { isOn(context, it) }.coerceAtLeast(0)

    /** The launcher entry in use, which the presets offered from the icon belong to. */
    fun launcher(context: Context): ComponentName = component(context, ALIASES[current(context)])

    /** The icon in use, for the presets offered from it. */
    fun icon(context: Context): Int = ICONS[current(context)]

    /** Switches to the icon for [seed], or for the wallpaper's colour when null. Does nothing if it's already on. */
    fun match(context: Context, seed: Long?) {
        val wanted = ALIASES[nearest(context, seed)]
        val manager = context.packageManager
        fun component(name: String) = component(context, name)
        fun isOn(name: String) = isOn(context, name)
        if (isOn(wanted) && ALIASES.none { it != wanted && isOn(it) }) return
        runCatching {
            // The new one first, so the app is never left without an icon to open it from.
            manager.setComponentEnabledSetting(
                component(wanted),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )
            ALIASES.filter { it != wanted }.forEach {
                manager.setComponentEnabledSetting(
                    component(it),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP,
                )
            }
            // The presets offered from the icon belong to a launcher entry, so they move to the new one, in its colour.
            PresetShortcuts.moveTo(context, component(wanted), ICONS[ALIASES.indexOf(wanted)])
        }.onFailure { Log.w("EigentoneIcon", "Couldn't switch the app icon", it) }
    }

    /** The swatch whose hue is closest to [seed]'s. */
    private fun nearest(context: Context, seed: Long?): Int {
        val colour = when {
            seed != null -> seed
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                androidx.compose.material3.dynamicLightColorScheme(context).primary.toArgb().toLong() and 0xFFFFFFFFL
            else -> return 0
        }
        if (seed != null && seed !in SeedScheme.SWATCHES && SeedScheme.picked(seed).saturation < GREY_SATURATION) return 0
        val hue = SeedScheme.hueOf(Color(colour.toInt()))
        return SeedScheme.SWATCHES.indices.minBy { i ->
            val d = abs(SeedScheme.hueOf(Color(SeedScheme.SWATCHES[i].toInt())) - hue)
            minOf(d, 360.0 - d)
        }
    }
}
