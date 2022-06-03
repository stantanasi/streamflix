package com.tanasi.sflix.fragments.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.sflix.adapters.SflixAdapter
import com.tanasi.sflix.models.Episode
import com.tanasi.sflix.models.Movie
import com.tanasi.sflix.models.Season
import com.tanasi.sflix.models.TvShow
import com.tanasi.sflix.services.SflixService
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {

    private val sflixService = SflixService.build()

    private val _state: MutableLiveData<State> = MutableLiveData(State.Searching)
    val state: LiveData<State> = _state

    sealed class State {
        object Searching : State()

        data class SuccessSearching(val results: List<SflixAdapter.Item>) : State()
    }


    fun search(query: String) = viewModelScope.launch {
        _state.value = State.Searching

        val document = sflixService.search(query.replace(" ", "-"))

        _state.value = State.SuccessSearching(
            document.select("div.flw-item").map {

                val isMovie = it.selectFirst("a")?.attr("href")?.contains("/movie/") ?: false

                val info = it
                    .select("div.film-detail > div.fd-infor > span")
                    .toList()
                    .map { element -> element.text() }
                    .takeIf { info -> info.size == 3 }

                val id = it.selectFirst("a")?.attr("href")?.substringAfterLast("-") ?: ""
                val title = it.select("h2.film-name").text()
                val released = info?.get(2) ?: ""
                val quality = info?.get(1) ?: ""
                val rating = info?.get(0)?.toDoubleOrNull()
                val poster = it.selectFirst("div.film-poster > img.film-poster-img").let { img ->
                    img?.attr("data-src") ?: img?.attr("src")
                } ?: ""

                when (isMovie) {
                    true -> Movie(
                        id = id,
                        title = title,
                        released = released,
                        quality = quality,
                        rating = rating,
                        poster = poster,
                    )
                    false -> TvShow(
                        id = id,
                        title = title,
                        quality = quality,
                        rating = rating,
                        poster = poster,

                        seasons = info?.get(2)?.let { lastEpisode ->
                            listOf(
                                Season(
                                    id = "",
                                    number = lastEpisode
                                        .substringAfter("S")
                                        .substringBefore(":")
                                        .toInt(),

                                    episodes = listOf(
                                        Episode(
                                            id = "",
                                            number = lastEpisode
                                                .substringAfter(":")
                                                .substringAfter("E")
                                                .toInt()
                                        )
                                    )
                                )
                            )
                        } ?: listOf(),
                    )
                }
            }
        )
    }
}