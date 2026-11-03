package com.phonons.app.engine

import android.app.ForegroundServiceStartNotAllowedException
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.phonons.app.R
import com.phonons.app.data.Repo
import com.phonons.app.ui.MainActivity

/** A home-screen widget showing the current preset, with power, Bypass and next preset buttons. */
class PhononsWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        manager.updateAppWidget(ids, views(context))
    }

    companion object {
        /** Cheap when no widget has been added, because the id list is empty. */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, PhononsWidget::class.java))
            if (ids.isNotEmpty()) manager.updateAppWidget(ids, views(context))
        }

        private fun views(context: Context): RemoteViews {
            val state = Repo.state.value
            return RemoteViews(context.packageName, R.layout.widget_phonons).apply {
                setTextViewText(R.id.widget_preset, if (state.enabled) state.current.name else context.getString(R.string.ui_mode_off))
                setTextViewText(R.id.widget_power, context.getString(if (state.enabled) R.string.engine_turn_off else R.string.widget_turn_on))
                setTextViewText(R.id.widget_bypass, context.getString(if (state.bypass) R.string.engine_resume else R.string.widget_bypass))
                setOnClickPendingIntent(
                    R.id.widget_preset,
                    PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE),
                )
                setOnClickPendingIntent(R.id.widget_power, action(context, WidgetReceiver.POWER, 1))
                setOnClickPendingIntent(R.id.widget_bypass, action(context, WidgetReceiver.BYPASS, 2))
                setOnClickPendingIntent(R.id.widget_next, action(context, WidgetReceiver.NEXT, 3))
            }
        }

        private fun action(context: Context, name: String, code: Int): PendingIntent = PendingIntent.getBroadcast(
            context, code,
            Intent(context, WidgetReceiver::class.java).setAction(name),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}

/**
 * Changes the shared state directly, so the buttons work whether or not the service is running. It
 * can't race with a service that's starting up and hasn't seen the new state yet.
 */
class WidgetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            POWER -> {
                val turnOn = !Repo.state.value.enabled
                Repo.update { it.copy(enabled = turnOn) }
                if (turnOn) start(context)
                AudioService.requestTileRefresh(context)
            }
            BYPASS -> Repo.update { it.copy(bypass = !it.bypass) }
            NEXT -> Repo.nextPreset()
        }
        PhononsWidget.refresh(context)
    }

    private fun start(context: Context) {
        try {
            AudioService.start(context)
        } catch (e: ForegroundServiceStartNotAllowedException) {
            // Android refused a background start, so open the app, which can start it.
            context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    companion object {
        const val POWER = "com.phonons.app.widget.POWER"
        const val BYPASS = "com.phonons.app.widget.BYPASS"
        const val NEXT = "com.phonons.app.widget.NEXT"
    }
}
