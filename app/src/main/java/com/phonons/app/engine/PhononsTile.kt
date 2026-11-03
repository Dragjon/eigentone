package com.phonons.app.engine

import android.app.ForegroundServiceStartNotAllowedException
import android.app.PendingIntent
import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.phonons.app.data.Repo
import com.phonons.app.ui.MainActivity

class PhononsTile : TileService() {

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
                startActivityAndCollapse(
                    PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE),
                )
            }
        }
        render(turnOn)
    }

    private fun render(on: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.subtitle = if (on) Repo.state.value.current.name else getString(com.phonons.app.R.string.ui_mode_off)
        tile.updateTile()
    }
}
