package com.alameen.fullquran

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/** Keeps the Quran playing in the background with notification / lock-screen controls. */
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = (application as QuranApp).controller.player
        val s = MediaSession.Builder(this, player).build()
        session = s
        addSession(s)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        session?.release()
        session = null
        super.onDestroy()
    }
}
