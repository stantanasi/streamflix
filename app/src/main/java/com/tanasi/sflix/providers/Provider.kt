package com.tanasi.sflix.providers

import com.tanasi.sflix.adapters.AppAdapter
import com.tanasi.sflix.fragments.player.PlayerFragment
import com.tanasi.sflix.models.*

interface Provider {

    val name: String
    val logo: String
    val url: String


    suspend fun getHome(): List<Category>

    suspend fun search(query: String): List<AppAdapter.Item>

    suspend fun getMovies(): List<Movie>

    suspend fun getTvShows(): List<TvShow>


    suspend fun getMovie(id: String): Movie


    suspend fun getTvShow(id: String): TvShow

    suspend fun getSeasonEpisodes(seasonId: String): List<Episode>


    suspend fun getGenre(id: String): Genre


    suspend fun getPeople(id: String): People


    suspend fun getVideo(id: String, videoType: PlayerFragment.VideoType): Video
}