package com.tanasi.sflix.fragments.movies

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.sflix.models.Movie
import com.tanasi.sflix.services.SflixService
import kotlinx.coroutines.launch

class MoviesViewModel : ViewModel() {

    private val sflixService = SflixService.build()

    private val _state = MutableLiveData<State>(State.Loading)
    val state: LiveData<State> = _state

    sealed class State {
        object Loading : State()

        data class SuccessLoading(val movies: List<Movie>) : State()
        data class FailedLoading(val error: Exception) : State()
    }

    fun getMovies() = viewModelScope.launch {
        _state.value = State.Loading

        _state.value = try {
            val document = sflixService.fetchMovies()

            val movies = document
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
                        title = it.select("h2.film-name").text(),
                        released = info.released ?: "",
                        quality = info.quality ?: "",
                        rating = info.rating,
                        poster = it.selectFirst("div.film-poster > img.film-poster-img")
                            .let { img ->
                                img?.attr("data-src") ?: img?.attr("src")
                            } ?: "",
                    )
                }

            State.SuccessLoading(movies)
        } catch (e: Exception) {
            State.FailedLoading(e)
        }
    }
}