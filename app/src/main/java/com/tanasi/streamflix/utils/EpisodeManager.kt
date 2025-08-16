package com.tanasi.streamflix.utils

import com.tanasi.streamflix.models.Video.Type.Episode

object EpisodeManager {
    private val episodes = mutableListOf<Episode>()
    var currentIndex = 0
        private set

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
    fun hasNextEpisode(): Boolean =
        currentIndex + 1 < episodes.size
}
