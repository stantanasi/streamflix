package com.tanasi.streamflix.database

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tanasi.streamflix.database.dao.EpisodeDao
import com.tanasi.streamflix.database.dao.MovieDao
import com.tanasi.streamflix.database.dao.SeasonDao
import com.tanasi.streamflix.database.dao.TvShowDao
import com.tanasi.streamflix.models.Episode
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.models.Season
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.utils.UserPreferences

@Database(
    entities = [
        Episode::class,
        Movie::class,
        Season::class,
        TvShow::class,
    ],
    version = 1,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun movieDao(): MovieDao

    abstract fun tvShowDao(): TvShowDao

    abstract fun seasonDao(): SeasonDao

    abstract fun episodeDao(): EpisodeDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        @UnstableApi
        fun setup(context: Context) {
            if (UserPreferences.currentProvider == null) return

            synchronized(this) {
                buildDatabase(context).also { INSTANCE = it }
            }
        }

        fun getInstance(context: Context) =
            INSTANCE ?: synchronized(this) {
                buildDatabase(context).also { INSTANCE = it }
            }

        @OptIn(UnstableApi::class)
        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                context = context.applicationContext,
                klass = AppDatabase::class.java,
                name = "${UserPreferences.currentProvider!!.name.lowercase()}.db"
            )
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build()
    }
}