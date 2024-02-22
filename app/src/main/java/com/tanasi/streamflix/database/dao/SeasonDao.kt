package com.tanasi.streamflix.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tanasi.streamflix.models.Season

@Dao
interface SeasonDao {

    @Query("SELECT * FROM seasons WHERE id = :id")
    fun getSeason(id: String): Season?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(seasons: List<Season>)
}