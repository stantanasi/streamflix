package com.tanasi.streamflix.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tanasi.streamflix.models.Movie

@Dao
interface MovieDao {

    @Query("SELECT * FROM movies WHERE isFavorite = 1")
    fun getFavoriteMovies(): List<Movie>

    @Query("SELECT * FROM movies WHERE id = :id")
    fun getMovie(id: String): Movie?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(movie: Movie)

    @Query("UPDATE movies SET isFavorite = :isFavorite WHERE id = :id")
    fun updateFavorite(id: String, isFavorite: Boolean)
}