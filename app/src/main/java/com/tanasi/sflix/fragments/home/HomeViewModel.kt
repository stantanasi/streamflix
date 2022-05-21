package com.tanasi.sflix.fragments.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.sflix.models.Movie
import com.tanasi.sflix.models.TvShow
import com.tanasi.sflix.services.SflixService
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val sflixService = SflixService.build()

    private val _state: MutableLiveData<State> = MutableLiveData(State.Loading)
    val state: LiveData<State> = _state

    sealed class State {
        object Loading : State()
        data class SuccessLoading(
            val trendingMovies: List<Movie>,
            val trendingTvShows: List<TvShow>
        ) : State()
    }

    fun fetchHome() = viewModelScope.launch {
        val document = sflixService.fetchHome()

        val trendingMovies = document
            .select("div#trending-movies")
            .select("div.flw-item")
            .map {
                val info = it
                    .select("div.film-detail > div.fd-infor > span")
                    .toList()
                    .map { element -> element.text() }
                    .takeIf { info -> info.size == 3 }

                Movie(
                    id = it.selectFirst("a")?.attr("href")?.substringAfterLast("-") ?: "",
                    title = it.select("h3.film-name").text(),
                    year = info?.get(2)?.toIntOrNull() ?: 0,
                    quality = info?.get(1) ?: "",
                    rating = info?.get(0)?.toDouble() ?: Double.NaN,
                    poster = it.selectFirst("div.film-poster > img.film-poster-img").let { img ->
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
                    .takeIf { info -> info.size == 3 }

                TvShow(
                    id = it.selectFirst("a")?.attr("href")?.substringAfterLast("-") ?: "",
                    title = it.select("h3.film-name").text(),
                    lastEpisode = info?.get(2) ?: "",
                    quality = info?.get(1) ?: "",
                    rating = info?.get(0)?.toDouble() ?: Double.NaN,
                    poster = it.selectFirst("div.film-poster > img.film-poster-img").let { img ->
                        img?.attr("data-src") ?: img?.attr("src")
                    } ?: "",
                )
            }

        _state.value = State.SuccessLoading(
            trendingMovies,
            trendingTvShows,
        )
    }
}