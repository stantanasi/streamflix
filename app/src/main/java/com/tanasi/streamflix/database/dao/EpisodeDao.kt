package com.tanasi.streamflix.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tanasi.streamflix.models.Episode

@Dao
interface EpisodeDao {

    @Query("SELECT * FROM episodes WHERE tvShow = :tvShowId ORDER BY season, number")
    fun getEpisodesByTvShowId(tvShowId: String): List<Episode>

    @Query("SELECT * FROM episodes WHERE season = :seasonId ORDER BY season, number")
    fun getEpisodesBySeasonId(seasonId: String): List<Episode>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(episodes: List<Episode>)

    @Query("UPDATE episodes SET isWatched = :isWatched WHERE id = :id")
    fun updateWatched(id: String, isWatched: Boolean)
}