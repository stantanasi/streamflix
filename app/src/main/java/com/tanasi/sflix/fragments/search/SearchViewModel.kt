package com.tanasi.sflix.fragments.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.sflix.adapters.SflixAdapter
import com.tanasi.sflix.models.*
import com.tanasi.sflix.services.SflixService
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {

    private val sflixService = SflixService.build()

    private val _state = MutableLiveData<State>(State.Searching)
    val state: LiveData<State> = _state

    sealed class State {
        object Searching : State()

        data class SuccessSearching(val results: List<Show>) : State()
        data class FailedSearching(val error: Exception) : State()
    }


    fun search(query: String) = viewModelScope.launch {
        _state.value = State.Searching

        _state.value = try {
            if (query.isEmpty()) {
                State.SuccessSearching(listOf())
            } else {
                val document = sflixService.search(query.replace(" ", "-"))

                State.SuccessSearching(
                    document.select("div.flw-item").map {
                        val isMovie = it.selectFirst("a")
                            ?.attr("href")
                            ?.contains("/movie/")
                            ?: false

                        val id = it.selectFirst("a")?.attr("href")?.substringAfterLast("-") ?: ""
                        val title = it.select("h2.film-name").text()
                        val poster =
                            it.selectFirst("div.film-poster > img.film-poster-img").let { img ->
                                img?.attr("data-src") ?: img?.attr("src")
                            } ?: ""

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
                                    id = id,
                                    title = title,
                                    released = info.released,
                                    quality = info.quality ?: "",
                                    rating = info.rating,
                                    poster = poster,
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
                                    id = id,
                                    title = title,
                                    quality = info.quality ?: "",
                                    rating = info.rating,
                                    poster = poster,

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
                    }
                )
            }
        } catch (e: Exception) {
            State.FailedSearching(e)
        }
    }
}