package com.tanasi.streamflix.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tanasi.streamflix.models.TvShow

@Dao
interface TvShowDao {

    @Query("SELECT * FROM tv_shows WHERE isFavorite = 1")
    fun getFavoriteTvShows(): List<TvShow>

    @Query("SELECT * FROM tv_shows WHERE id = :id")
    fun getTvShowById(id: String): TvShow?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(tvShow: TvShow)

    @Query("UPDATE tv_shows SET isFavorite = :isFavorite WHERE id = :id")
    fun updateFavorite(id: String, isFavorite: Boolean)
}