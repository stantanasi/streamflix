package com.tanasi.streamflix.utils

import com.tanasi.streamflix.database.AppDatabase
import com.tanasi.streamflix.database.dao.EpisodeDao
import com.tanasi.streamflix.models.Video
import com.tanasi.streamflix.models.Video.Type.Episode

object EpisodeManager {


    private lateinit var episodeDao: EpisodeDao
    private val episodes = mutableListOf<Episode>()
    var currentIndex = 0
        private set

    fun init(database: AppDatabase) {
        episodeDao = database.episodeDao()
    }
    // Add episodes per season, clear every time we enter a new show,season, etc
    fun addEpisodes(list: List<Episode>) {
        episodes.clear()
        episodes.addAll(list)
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
    fun setEpisodesForSeason(tvShowId: String, seasonNumber: Int, episodeId: String) {
        val seasonEpisodes = episodeDao.getEpisodesByTvShowId(tvShowId)
            .filter { it.season?.number == seasonNumber }

        episodes.clear()
        seasonEpisodes.forEach { ep ->
            episodes.add(
                Video.Type.Episode(
                    id = ep.id,
                    number = ep.number,
                    title = ep.title,
                    poster = ep.poster,
                    tvShow = Video.Type.Episode.TvShow(
                        id = ep.tvShow?.id ?: "",
                        title = ep.tvShow?.title ?: "",
                        poster = ep.tvShow?.poster,
                        banner = ep.tvShow?.banner
                    ),
                    season = Video.Type.Episode.Season(
                        number = ep.season?.number ?: 0,
                        title = ep.season?.title
                    )
                )
            )
        }
        currentIndex = episodes.indexOfFirst { it.id == episodeId }.takeIf { it >= 0 } ?: 0
    }




}
