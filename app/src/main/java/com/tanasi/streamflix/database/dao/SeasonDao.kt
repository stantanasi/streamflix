package com.tanasi.streamflix.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tanasi.streamflix.models.Season
import kotlinx.coroutines.flow.Flow

@Dao
interface SeasonDao {

    @Query("SELECT * FROM seasons WHERE id = :id")
    fun getById(id: String): Season?

    @Query("SELECT * FROM seasons WHERE id = :id")
    fun getByIdAsFlow(id: String): Flow<Season?>

    @Query("SELECT * FROM seasons WHERE tvShow = :tvShowId")
    fun getByTvShowIdAsFlow(tvShowId: String): Flow<List<Season>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(seasons: List<Season>)
}