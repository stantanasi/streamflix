package com.tanasi.streamflix

import android.app.Application
import android.content.Context
import com.tanasi.streamflix.providers.AniWorldProvider
import com.tanasi.streamflix.providers.SerienStreamProvider

class StreamFlixApp: Application() {
    companion object {
        lateinit var instance: StreamFlixApp
            private set
    }
    override fun onCreate() {
        super.onCreate()
        instance = this
        SerienStreamProvider.initialize(this)
        AniWorldProvider.initialize(this)
    }
}