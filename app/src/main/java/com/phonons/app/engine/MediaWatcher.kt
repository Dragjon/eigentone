package com.phonons.app.engine

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.util.Log

/**
 * Exists only because Android requires it before an app may ask which media is playing. It ignores
 * every notification, and it's only active if the user grants access.
 */
class PhononsListener : NotificationListenerService()

/** Reports which app is playing, for per-app presets. Does nothing until access has been granted. */
class MediaWatcher(private val context: Context, private val onPlaying: (String) -> Unit) {
    private val handler = Handler(Looper.getMainLooper())
    private val component = ComponentName(context, PhononsListener::class.java)
    private val sessions = context.getSystemService(MediaSessionManager::class.java)
    private val watched = mutableMapOf<MediaController, MediaController.Callback>()
    private var attached = false

    private val sessionsChanged = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        track(controllers.orEmpty())
    }

    /**
     * Safe to call repeatedly, so access granted while the service is running gets picked up. Android
     * drops the listener when access is revoked, so that has to be noticed for a re-grant to work.
     */
    fun attachIfAllowed() {
        handler.post {
            val allowed = isAllowed(context)
            if (attached && !allowed) detach()
            if (attached || !allowed) return@post
            try {
                sessions.addOnActiveSessionsChangedListener(sessionsChanged, component, handler)
                attached = true
                track(sessions.getActiveSessions(component))
            } catch (e: SecurityException) {
                Log.w(TAG, "Media access not available", e)
            }
        }
    }

    fun release() {
        handler.post { detach() }
    }

    private fun detach() {
        if (attached) runCatching { sessions.removeOnActiveSessionsChangedListener(sessionsChanged) }
        attached = false
        untrack()
    }

    private fun track(controllers: List<MediaController>) {
        untrack()
        controllers.forEach { controller ->
            val callback = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) = report(controllers)
            }
            controller.registerCallback(callback, handler)
            watched[controller] = callback
        }
        report(controllers)
    }

    private fun untrack() {
        watched.forEach { (controller, callback) -> controller.unregisterCallback(callback) }
        watched.clear()
    }

    /** Pausing keeps the last app, so resuming can't undo a preset chosen by hand in between. */
    private fun report(controllers: List<MediaController>) {
        val playing = controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: return
        onPlaying(playing.packageName)
    }

    companion object {
        private const val TAG = "PhononsMedia"

        fun isAllowed(context: Context): Boolean =
            context.getSystemService(NotificationManager::class.java)
                .isNotificationListenerAccessGranted(ComponentName(context, PhononsListener::class.java))
    }
}
