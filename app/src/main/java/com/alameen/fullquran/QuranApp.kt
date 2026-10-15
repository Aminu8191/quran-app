package com.alameen.fullquran

import android.app.Application

class QuranApp : Application() {
    lateinit var controller: PlayerController
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        AppSettings.init(this)
        AudioStore.init(this)
        controller = PlayerController(this)
        controller.restore()
        controller.start()
        AdManager.init(this)      // SDK + remote config run on background threads
    }

    companion object {
        lateinit var instance: QuranApp
            private set
    }
}
