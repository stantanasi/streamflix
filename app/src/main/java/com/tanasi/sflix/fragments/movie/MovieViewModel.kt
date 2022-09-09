package com.tanasi.sflix.fragments.movie

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.sflix.models.Movie
import com.tanasi.sflix.models.People
import com.tanasi.sflix.models.Server
import com.tanasi.sflix.services.SflixService
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


    fun fetchMovie(id: String) = viewModelScope.launch {
        _state.value = State.Loading

        _state.value = try {
            val document = sflixService.fetchMovie(id)

            State.SuccessLoading(
                Movie(
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

                    casts = document.select("div.elements > .row > div > .row-line")
                        .find { it?.select(".type")?.text()?.contains("Casts") ?: false }
                        ?.select("a")
                        ?.map {
                            People(
                                slug = it.attr("href").substringAfter("/cast/"),
                                name = it.text(),
                            )
                        } ?: listOf(),
                    servers = sflixService.fetchMovieServers(id)
                        .select("a")
                        .map {
                            Server(
                                id = it.attr("data-id"),
                                name = it.selectFirst("span")?.text()?.trim() ?: "",
                            )
                        },
                )
            )
        } catch (e: Exception) {
            State.FailedLoading(e)
        }
    }
}