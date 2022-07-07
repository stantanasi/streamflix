package com.tanasi.sflix.fragments.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.sflix.models.Episode
import com.tanasi.sflix.models.Movie
import com.tanasi.sflix.models.Season
import com.tanasi.sflix.models.TvShow
import com.tanasi.sflix.services.SflixService
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val sflixService = SflixService.build()

    private val _state = MutableLiveData<State>(State.Loading)
    val state: LiveData<State> = _state

    sealed class State {
        object Loading : State()

        data class SuccessLoading(
            val trendingMovies: List<Movie>,
            val trendingTvShows: List<TvShow>,
            val latestMovies: List<Movie>,
            val latestTvShows: List<TvShow>,
        ) : State()
        data class FailedLoading(val error: Exception) : State()
    }

    fun fetchHome() = viewModelScope.launch {
        _state.value = State.Loading

        _state.value = try {
            val document = sflixService.fetchHome()

            val trendingMovies = document
                .select("div#trending-movies")
                .select("div.flw-item")
                .map {
                    val info = it
                        .select("div.film-detail > div.fd-infor > span")
                        .toList()
                        .map { element -> element.text() }
                        .let { info ->
                            object {
                                val released = when (info.size) {
                                    1 -> info[0] ?: ""
                                    2 -> info[1] ?: ""
                                    3 -> info[2] ?: ""
                                    else -> null
                                }
                                val quality = when (info.size) {
                                    3 -> info[1] ?: ""
                                    else -> null
                                }
                                val rating = when (info.size) {
                                    2 -> info[0].toDoubleOrNull()
                                    3 -> info[0].toDoubleOrNull()
                                    else -> null
                                }
                            }
                        }

                    Movie(
                        id = it.selectFirst("a")?.attr("href")?.substringAfterLast("-") ?: "",
                        title = it.select("h3.film-name").text(),
                        released = info.released ?: "",
                        quality = info.quality ?: "",
                        rating = info.rating,
                        poster = it.selectFirst("div.film-poster > img.film-poster-img")
                            .let { img ->
                                img?.attr("data-src") ?: img?.attr("src")
                            } ?: "",
                    )
                }

            val trendingTvShows = document
                .select("div#trending-tv")
                .select("div.flw-item")
                .map {
                    val info = it
                        .select("div.film-detail > div.fd-infor > span")
                        .toList()
                        .map { element -> element.text() }
                        .let { info ->
                            object {
                                val quality = when (info.size) {
                                    3 -> info[1] ?: ""
                                    else -> null
                                }
                                val rating = when (info.size) {
                                    2 -> info[0].toDoubleOrNull()
                                    3 -> info[0].toDoubleOrNull()
                                    else -> null
                                }
                                val lastEpisode = when (info.size) {
                                    1 -> info[0] ?: ""
                                    2 -> info[1] ?: ""
                                    3 -> info[2] ?: ""
                                    else -> null
                                }
                            }
                        }

                    TvShow(
                        id = it.selectFirst("a")?.attr("href")?.substringAfterLast("-") ?: "",
                        title = it.select("h3.film-name").text(),
                        quality = info.quality ?: "",
                        rating = info.rating,
                        poster = it.selectFirst("div.film-poster > img.film-poster-img")
                            .let { img ->
                                img?.attr("data-src") ?: img?.attr("src")
                            } ?: "",

                        seasons = info.lastEpisode?.let { lastEpisode ->
                            listOf(
                                Season(
                                    id = "",
                                    number = lastEpisode
                                        .substringAfter("S")
                                        .substringBefore(":")
                                        .toIntOrNull() ?: 0,

                                    episodes = listOf(
                                        Episode(
                                            id = "",
                                            number = lastEpisode
                                                .substringAfter(":")
                                                .substringAfter("E")
                                                .toIntOrNull() ?: 0,
                                        )
                                    )
                                )
                            )
                        } ?: listOf()
                    )
                }

            val latestMovies = document
                .select(".section-id-02:has(h2:matchesOwn(Latest Movies))")
                .select("div.flw-item")
                .map {
                    val info = it
                        .select("div.film-detail > div.fd-infor > span")
                        .toList()
                        .map { element -> element.text() }
                        .let { info ->
                            object {
                                val released = when (info.size) {
                                    1 -> info[0] ?: ""
                                    2 -> info[1] ?: ""
                                    3 -> info[2] ?: ""
                                    else -> null
                                }
                                val quality = when (info.size) {
                                    3 -> info[1] ?: ""
                                    else -> null
                                }
                                val rating = when (info.size) {
                                    2 -> info[0].toDoubleOrNull()
                                    3 -> info[0].toDoubleOrNull()
                                    else -> null
                                }
                            }
                        }

                    Movie(
                        id = it.selectFirst("a")?.attr("href")?.substringAfterLast("-") ?: "",
                        title = it.select("h3.film-name").text(),
                        released = info.released ?: "",
                        quality = info.quality ?: "",
                        rating = info.rating,
                        poster = it.selectFirst("div.film-poster > img.film-poster-img")
                            .let { img ->
                                img?.attr("data-src") ?: img?.attr("src")
                            } ?: "",
                    )
                }

            val latestTvShows = document
                .select(".section-id-02:has(h2:matchesOwn(Latest TV Shows))")
                .select("div.flw-item")
                .map {
                    val info = it
                        .select("div.film-detail > div.fd-infor > span")
                        .toList()
                        .map { element -> element.text() }
                        .let { info ->
                            object {
                                val quality = when (info.size) {
                                    3 -> info[1] ?: ""
                                    else -> null
                                }
                                val rating = when (info.size) {
                                    2 -> info[0].toDoubleOrNull()
                                    3 -> info[0].toDoubleOrNull()
                                    else -> null
                                }
                                val lastEpisode = when (info.size) {
                                    1 -> info[0] ?: ""
                                    2 -> info[1] ?: ""
                                    3 -> info[2] ?: ""
                                    else -> null
                                }
                            }
                        }

                    TvShow(
                        id = it.selectFirst("a")?.attr("href")?.substringAfterLast("-") ?: "",
                        title = it.select("h3.film-name").text(),
                        quality = info.quality ?: "",
                        rating = info.rating,
                        poster = it.selectFirst("div.film-poster > img.film-poster-img")
                            .let { img ->
                                img?.attr("data-src") ?: img?.attr("src")
                            } ?: "",

                        seasons = info.lastEpisode?.let { lastEpisode ->
                            listOf(
                                Season(
                                    id = "",
                                    number = lastEpisode
                                        .substringAfter("S")
                                        .substringBefore(":")
                                        .toIntOrNull() ?: 0,

                                    episodes = listOf(
                                        Episode(
                                            id = "",
                                            number = lastEpisode
                                                .substringAfter(":")
                                                .substringAfter("E")
                                                .toIntOrNull() ?: 0,
                                        )
                                    )
                                )
                            )
                        } ?: listOf()
                    )
                }

            State.SuccessLoading(
                trendingMovies,
                trendingTvShows,
                latestMovies,
                latestTvShows,
            )
        } catch (e: Exception) {
            State.FailedLoading(e)
        }
    }
}