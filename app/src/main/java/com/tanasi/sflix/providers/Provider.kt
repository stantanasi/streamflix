package com.tanasi.sflix.providers

import com.tanasi.sflix.fragments.player.PlayerFragment
import com.tanasi.sflix.models.*

interface Provider {

    var name: String
    var logo: String


    suspend fun getHome(): List<Category>

    suspend fun search(query: String): List<Show>

    suspend fun getMovies(): List<Movie>

    suspend fun getTvShows(): List<TvShow>


    suspend fun getMovie(id: String): Movie


    suspend fun getTvShow(id: String): TvShow

    suspend fun getSeasonEpisodes(seasonId: String): List<Episode>


    suspend fun getPeople(id: String): People


    suspend fun getVideo(id: String, videoType: PlayerFragment.VideoType): Video
}