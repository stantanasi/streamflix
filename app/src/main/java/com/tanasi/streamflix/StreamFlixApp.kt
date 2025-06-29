package com.tanasi.streamflix

import android.app.Application
import android.content.Context
import com.tanasi.streamflix.providers.AniWorldProvider
import com.tanasi.streamflix.providers.SerienStreamProvider

class StreamFlixApp: Application() {
    override fun onCreate() {
        super.onCreate()
        SerienStreamProvider.initialize(this)
        AniWorldProvider.initialize(this)
    }
}