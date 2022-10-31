package com.tanasi.sflix.fragments.people

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.sflix.models.*
import com.tanasi.sflix.services.SflixService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PeopleViewModel : ViewModel() {

    private val sflixService = SflixService.build()

    private val _state = MutableLiveData<State>(State.Loading)
    val state: LiveData<State> = _state

    sealed class State {
        object Loading : State()

        data class SuccessLoading(val people: People) : State()
        data class FailedLoading(val error: Exception) : State()
    }


    fun getPeopleById(peopleId: String) = viewModelScope.launch(Dispatchers.IO) {
        _state.postValue(State.Loading)

        try {
            val document = sflixService.getPeopleBySlug(peopleId)

            val people = People(
                id = peopleId,
                name = document.selectFirst("h2.cat-heading")?.text() ?: "",

                filmography = document.select("div.flw-item").map {
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
                },
            )

            _state.postValue(State.SuccessLoading(people))
        } catch (e: Exception) {
            _state.postValue(State.FailedLoading(e))
        }
    }
}