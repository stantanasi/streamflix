package com.tanasi.streamflix.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tanasi.streamflix.models.Movie

@Dao
interface MovieDao {

    @Query("SELECT * FROM movies WHERE id = :id")
    fun getById(id: String): Movie?

    @Query("SELECT * FROM movies WHERE isFavorite = 1")
    fun getFavorites(): List<Movie>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(movie: Movie)

    @Update
    fun update(movie: Movie)

    @Query("UPDATE movies SET isFavorite = :isFavorite WHERE id = :id")
    fun updateFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE movies SET isWatched = :isWatched WHERE id = :id")
    fun updateWatched(id: String, isWatched: Boolean)
}