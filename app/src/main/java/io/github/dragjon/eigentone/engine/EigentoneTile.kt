package io.github.dragjon.eigentone.engine

import android.app.ForegroundServiceStartNotAllowedException
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import io.github.dragjon.eigentone.data.Repo
import io.github.dragjon.eigentone.ui.MainActivity

class EigentoneTile : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        render(Repo.state.value.enabled)
    }

    override fun onClick() {
        super.onClick()
        val turnOn = !Repo.state.value.enabled
        Repo.update { it.copy(enabled = turnOn) }
        if (turnOn) {
            try {
                AudioService.start(this)
            } catch (e: ForegroundServiceStartNotAllowedException) {
                // Android refused a background start, so open the app, which can start it.
                val intent = Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startActivityAndCollapse(
                        PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE),
                    )
                } else {
                    @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
                    startActivityAndCollapse(intent)
                }
            }
        }
        render(turnOn)
    }

    private fun render(on: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.subtitle = if (on) Repo.state.value.current.name else getString(io.github.dragjon.eigentone.R.string.ui_mode_off)
        tile.updateTile()
    }
}
