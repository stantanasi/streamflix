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

    @Query("SELECT * FROM movies WHERE lastEngagementTimeUtcMillis IS NOT NULL ORDER BY lastEngagementTimeUtcMillis DESC")
    fun getWatchingMovies(): List<Movie>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(movie: Movie)

    @Update
    fun update(movie: Movie)

    fun save(movie: Movie) = getById(movie.id)
        ?.let { update(movie) }
        ?: insert(movie)
}