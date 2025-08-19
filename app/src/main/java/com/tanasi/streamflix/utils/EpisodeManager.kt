package com.tanasi.streamflix.utils

import com.tanasi.streamflix.database.AppDatabase
import com.tanasi.streamflix.database.dao.EpisodeDao
import com.tanasi.streamflix.models.Video
import com.tanasi.streamflix.models.Video.Type.Episode

object EpisodeManager {
    private val episodes = mutableListOf<Episode>()
    var currentIndex = 0
        private set

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




}
