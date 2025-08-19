package com.tanasi.streamflix

import android.app.Application
import android.content.Context
import com.tanasi.streamflix.database.AppDatabase
import com.tanasi.streamflix.providers.AniWorldProvider
import com.tanasi.streamflix.providers.SerienStreamProvider
import com.tanasi.streamflix.utils.EpisodeManager
import com.tanasi.streamflix.utils.UserPreferences // <-- IMPORT AGGIUNTO

class StreamFlixApp : Application() {
    companion object {
        lateinit var instance: StreamFlixApp
            private set
        lateinit var database: AppDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize UserPreferences first
        UserPreferences.setup(this)

        // Now you can safely build the database
        database = AppDatabase.getInstance(this)
        EpisodeManager.init(database) // initialize EpisodeManager with the database

        SerienStreamProvider.initialize(this)
        AniWorldProvider.initialize(this)
    }
}
