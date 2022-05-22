package com.tanasi.sflix.fragments.movie

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.sflix.models.Movie
import com.tanasi.sflix.models.Server
import com.tanasi.sflix.services.SflixService
import kotlinx.coroutines.launch

class MovieViewModel : ViewModel() {

    private val sflixService = SflixService.build()

    private val _state: MutableLiveData<State> = MutableLiveData(State.Loading)
    val state: LiveData<State> = _state

    sealed class State {
        object Loading : State()
        data class SuccessLoading(val movie: Movie) : State()
    }

    fun fetchMovie(id: String) = viewModelScope.launch {
        _state.value = State.Loading

        val document = sflixService.fetchMovie(id)

        var released = ""
        var runtime: Int? = null

        document.select("div.elements > .row > div > .row-line").forEach { element ->
            val type = element?.select(".type")?.text() ?: return@forEach
            when {
                type.contains("Released") -> released = element.ownText().trim()
                type.contains("Duration") ->
                    runtime = element.ownText()
                        .removeSuffix("min")
                        .trim()
                        .toIntOrNull()
            }
        }

        _state.value = State.SuccessLoading(
            Movie(
                id = id,
                title = document.selectFirst("h2.heading-name")?.text() ?: "",
                overview = document.selectFirst("div.description")?.ownText() ?: "",
                released = released,
                runtime = runtime,
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
    }
}