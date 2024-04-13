package com.tanasi.streamflix.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tanasi.streamflix.models.Episode
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodeDao {

    @Query("SELECT * FROM episodes WHERE id = :id")
    fun getById(id: String): Episode?

    @Query("SELECT * FROM episodes WHERE id IN (:ids)")
    fun getByIds(ids: List<String>): List<Episode>

    @Query("SELECT * FROM episodes WHERE id IN (:ids)")
    fun getByIdsAsFlow(ids: List<String>): Flow<List<Episode>>

    @Query("SELECT * FROM episodes WHERE tvShow = :tvShowId ORDER BY season, number")
    fun getByTvShowId(tvShowId: String): List<Episode>

    @Query("SELECT * FROM episodes WHERE tvShow = :tvShowId ORDER BY season, number")
    fun getByTvShowIdAsFlow(tvShowId: String): Flow<List<Episode>>

    @Query("SELECT * FROM episodes WHERE season = :seasonId ORDER BY season, number")
    fun getBySeasonId(seasonId: String): List<Episode>

    @Query("SELECT * FROM episodes WHERE lastEngagementTimeUtcMillis IS NOT NULL ORDER BY lastEngagementTimeUtcMillis DESC")
    fun getWatchingEpisodes(): Flow<List<Episode>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(episode: Episode)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(episodes: List<Episode>)

    @Update
    fun update(episode: Episode)

    fun save(episode: Episode) = getById(episode.id)
        ?.let { update(episode) }
        ?: insert(episode)

    @Query("""
        UPDATE episodes
        SET isWatched = 0
        WHERE id IN (
            SELECT episode.id
            FROM episodes episode
            LEFT JOIN seasons season ON episode.season = season.id
            JOIN (
                  SELECT episode.tvShow AS tvShow, season.number AS seasonNumber, episode.number AS number
                  FROM episodes episode
                  LEFT JOIN seasons season ON episode.season = season.id
                  WHERE episode.id = :id
            ) episode2 ON (episode.tvShow = episode2.tvShow AND (season.number > episode2.seasonNumber OR (season.number = episode2.seasonNumber AND episode.number > episode2.number)))
        )
    """)
    fun resetProgressionFromEpisode(id: String)
}