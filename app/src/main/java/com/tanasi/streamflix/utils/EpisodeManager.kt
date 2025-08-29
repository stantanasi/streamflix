package com.tanasi.streamflix.utils

import android.content.Context
import com.tanasi.streamflix.database.AppDatabase
import com.tanasi.streamflix.database.dao.EpisodeDao
import com.tanasi.streamflix.models.Video
import com.tanasi.streamflix.models.Video.Type.Episode
import kotlin.collections.map

object EpisodeManager {
    private val episodes = mutableListOf<Episode>()
    var currentIndex = 0
        private set

    fun addEpisodes(list: List<Episode>) {
        episodes.clear()
        episodes.addAll(list)
        currentIndex = 0
    }

    fun addEpisodesFromDb(type: Video.Type.Episode, database: AppDatabase){
        val tvShowId = type.tvShow.id
        val seasonNumber = type.season.number
        val episodesFromDb = database.episodeDao().getByTvShowIdAndSeasonNumber(tvShowId, seasonNumber)
        if (!episodesFromDb.isEmpty()){
            addEpisodes(convertToVideoTypeEpisodes(episodesFromDb, database, seasonNumber));

        }
    }
    fun clearEpisodes(){
        episodes.clear()
        currentIndex = 0
    }
    fun setCurrentEpisode(episode: Episode) {
        currentIndex = episodes.indexOfFirst { it.id == episode.id }
    }

    fun getCurrentEpisode(): Episode? =
        episodes.getOrNull(currentIndex)

    fun getNextEpisode(): Episode? {
        if (currentIndex + 1 < episodes.size) {
            currentIndex++
            return episodes[currentIndex]
        }
        return null
    }
    fun getPreviousEpisode(): Episode? {
        if (currentIndex -1 >= 0){
            currentIndex--
            return episodes[currentIndex]
        }
        return null
    }
    fun hasPreviousEpisode(): Boolean {
        return currentIndex > 0
    }

    fun hasNextEpisode(): Boolean {
        return currentIndex < episodes.size - 1
    }

    fun listIsEmpty(episode: Episode): Boolean{
        return episodes.isEmpty() || return episodes.indexOf(episode) == -1
    }

    fun convertToVideoTypeEpisodes(episodes: List<com.tanasi.streamflix.models.Episode>, database: AppDatabase, seasonNumber: Int): List<Episode> {
        val videoEpisodes = episodes.map { ep ->
            val seasonId = ep.season?.id ?: ""
            val tvShowId = ep.tvShow?.id ?: ""
            val seasonFromDb = database.seasonDao().getById(seasonId)
            val tvShowFromDb = database.tvShowDao().getById(tvShowId)
            Episode(
                id = ep.id,
                number = ep.number,
                title = ep.title,
                poster = ep.poster,
                tvShow = Episode.TvShow(
                    id = tvShowId,
                    title = tvShowFromDb?.title ?: "",
                    poster = tvShowFromDb?.poster ?: ep.tvShow?.poster,
                    banner = tvShowFromDb?.banner ?: ep.tvShow?.banner
                ),
                season = Episode.Season(
                    number = seasonFromDb?.number ?: seasonNumber,
                    title = seasonFromDb?.title ?: ep.season?.title
                )
            )
        }
        return videoEpisodes
    }



}
