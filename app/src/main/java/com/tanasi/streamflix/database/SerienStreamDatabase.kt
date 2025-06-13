package com.tanasi.streamflix.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tanasi.streamflix.database.dao.TvShowDao
import com.tanasi.streamflix.models.TvShow

@Database(entities = [TvShow::class], version = 4)
@TypeConverters(Converters::class)
abstract class SerienStreamDatabase: RoomDatabase() {
    abstract fun tvShowDao(): TvShowDao

    companion object {
        @Volatile private var instance: SerienStreamDatabase? = null

        fun getInstance(context: Context): SerienStreamDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SerienStreamDatabase::class.java,
                    "serien_stream.db"
                )
                .build()
                .also { instance = it }
            }
        }
    }
}