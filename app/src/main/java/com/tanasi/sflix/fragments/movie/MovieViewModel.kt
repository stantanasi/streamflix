package com.tanasi.sflix.fragments.movie

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.sflix.models.*
import com.tanasi.sflix.services.SflixService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MovieViewModel : ViewModel() {

    private val sflixService = SflixService.build()

    private val _state = MutableLiveData<State>(State.Loading)
    val state: LiveData<State> = _state

    sealed class State {
        object Loading : State()

        data class SuccessLoading(val movie: Movie) : State()
        data class FailedLoading(val error: Exception) : State()
    }


    fun getMovieById(id: String) = viewModelScope.launch(Dispatchers.IO) {
        _state.postValue(State.Loading)

        try {
            val document = sflixService.getMovieById(id)

            val movie = Movie(
                id = id,
                title = document.selectFirst("h2.heading-name")?.text() ?: "",
                overview = document.selectFirst("div.description")?.ownText() ?: "",
                released = document.select("div.elements > .row > div > .row-line")
                    .find { it?.select(".type")?.text()?.contains("Released") ?: false }
                    ?.ownText()
                    ?.trim()
                    ?: "",
                runtime = document.select("div.elements > .row > div > .row-line")
                    .find { it?.select(".type")?.text()?.contains("Duration") ?: false }
                    ?.ownText()
                    ?.removeSuffix("min")
                    ?.trim()
                    ?.toIntOrNull(),
                youtubeTrailerId = document.selectFirst("iframe#iframe-trailer")
                    ?.attr("data-src")
                    ?.substringAfterLast("/"),
                quality = document.selectFirst(".fs-item > .quality")?.text()
                    ?.trim() ?: "",
                rating = document.selectFirst(".fs-item > .imdb")?.text()
                    ?.trim()
                    ?.removePrefix("IMDB:")
                    ?.toDoubleOrNull(),
                poster = document.selectFirst("div.detail_page-watch img.film-poster-img")
                    ?.attr("src"),
                banner = document.selectFirst("div.detail-container > div.cover_follow")
                    ?.attr("style")
                    ?.substringAfter("background-image: url(")
                    ?.substringBefore(");"),

                genres = document.select("div.elements > .row > div > .row-line")
                    .find { it?.select(".type")?.text()?.contains("Genre") ?: false }
                    ?.select("a")
                    ?.map {
                        Genre(
                            id = it.attr("href").substringAfter("/genre/"),
                            name = it.text(),
                        )
                    } ?: listOf(),
                cast = document.select("div.elements > .row > div > .row-line")
                    .find { it?.select(".type")?.text()?.contains("Casts") ?: false }
                    ?.select("a")
                    ?.map {
                        People(
                            id = it.attr("href").substringAfter("/cast/"),
                            name = it.text(),
                        )
                    } ?: listOf(),
                recommendations = document
                    .select("div.film_related")
                    .select("div.flw-item")
                    .map {
                        val isMovie = it.selectFirst("a")
                            ?.attr("href")
                            ?.contains("/movie/")
                            ?: false

                        when (isMovie) {
                            true -> {
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
                                    id = it
                                        .selectFirst("a")
                                        ?.attr("href")
                                        ?.substringAfterLast("-")
                                        ?: "",
                                    title = it.select("h3.film-name").text(),
                                    released = info.released,
                                    quality = info.quality ?: "",
                                    rating = info.rating,
                                    poster = it
                                        .selectFirst("div.film-poster > img.film-poster-img")
                                        .let { img ->
                                            img?.attr("data-src") ?: img?.attr("src")
                                        }
                                        ?: "",
                                )
                            }
                            false -> {
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
                                    id = it
                                        .selectFirst("a")
                                        ?.attr("href")
                                        ?.substringAfterLast("-")
                                        ?: "",
                                    title = it.select("h3.film-name").text(),
                                    quality = info.quality ?: "",
                                    rating = info.rating,
                                    poster = it
                                        .selectFirst("div.film-poster > img.film-poster-img")
                                        .let { img ->
                                            img?.attr("data-src") ?: img?.attr("src")
                                        }
                                        ?: "",

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
                                    } ?: listOf(),
                                )
                            }
                        }
                    },
            )

            _state.postValue(State.SuccessLoading(movie))
        } catch (e: Exception) {
            _state.postValue(State.FailedLoading(e))
        }
    }
}