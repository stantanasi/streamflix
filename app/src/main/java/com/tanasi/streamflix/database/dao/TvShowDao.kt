package com.tanasi.streamflix.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tanasi.streamflix.models.TvShow
import kotlinx.coroutines.flow.Flow

@Dao
interface TvShowDao {

    @Query("SELECT * FROM tv_shows WHERE id = :id")
    fun getById(id: String): TvShow?

    @Query("SELECT * FROM tv_shows WHERE id = :id")
    fun getByIdAsFlow(id: String): Flow<TvShow?>

    @Query("SELECT * FROM tv_shows WHERE id IN (:ids)")
    fun getByIds(ids: List<String>): Flow<List<TvShow>>

    @Query("SELECT * FROM tv_shows WHERE isFavorite = 1")
    fun getFavorites(): Flow<List<TvShow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(tvShow: TvShow)

    @Update
    fun update(tvShow: TvShow)

    fun save(tvShow: TvShow) = getById(tvShow.id)
        ?.let { update(tvShow) }
        ?: insert(tvShow)
}