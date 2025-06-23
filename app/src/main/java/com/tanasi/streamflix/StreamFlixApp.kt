package com.tanasi.streamflix

import android.app.Application
import android.content.Context
import com.tanasi.streamflix.providers.AniWorldProvider
import com.tanasi.streamflix.providers.SerienStreamProvider

class StreamFlixApp: Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        SerienStreamProvider.initialize(this)
        AniWorldProvider.initialize(this)
    }
    companion object {
        lateinit var appContext: Context
            private set
    }
}