package com.tanasi.sflix.fragments.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.sflix.models.*
import com.tanasi.sflix.services.SflixService
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val sflixService = SflixService.build()

    private val _state = MutableLiveData<State>(State.Loading)
    val state: LiveData<State> = _state

    sealed class State {
        object Loading : State()

        data class SuccessLoading(val categories: List<Category>) : State()
        data class FailedLoading(val error: Exception) : State()
    }


    fun getHome() = viewModelScope.launch {
        _state.value = State.Loading

        _state.value = try {
            val document = sflixService.getHome()

            val categories = mutableListOf<Category>()

            categories.add(
                Category(
                    name = "Trending Movies",
                    list = document
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
                                id = it.selectFirst("a")
                                    ?.attr("href")
                                    ?.substringAfterLast("-")
                                    ?: "",
                                title = it.select("h3.film-name").text(),
                                released = info.released ?: "",
                                quality = info.quality ?: "",
                                rating = info.rating,
                                poster = it.selectFirst("div.film-poster > img.film-poster-img")
                                    ?.attr("data-src")
                                    ?: "",
                            )
                        },
                )
            )

            categories.add(
                Category(
                    name = "Trending TV Shows",
                    list = document
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
                                id = it.selectFirst("a")
                                    ?.attr("href")
                                    ?.substringAfterLast("-")
                                    ?: "",
                                title = it.select("h3.film-name").text(),
                                quality = info.quality ?: "",
                                rating = info.rating,
                                poster = it.selectFirst("div.film-poster > img.film-poster-img")
                                    ?.attr("data-src")
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
                                } ?: listOf()
                            )
                        },
                )
            )

            categories.add(
                Category(
                    name = "Latest Movies",
                    list = document
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
                                id = it.selectFirst("a")
                                    ?.attr("href")
                                    ?.substringAfterLast("-")
                                    ?: "",
                                title = it.select("h3.film-name").text(),
                                released = info.released ?: "",
                                quality = info.quality ?: "",
                                rating = info.rating,
                                poster = it.selectFirst("div.film-poster > img.film-poster-img")
                                    ?.attr("data-src")
                                    ?: "",
                            )
                        },
                )
            )

            categories.add(
                Category(
                    name = "Latest TV Shows",
                    list = document
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
                                id = it.selectFirst("a")
                                    ?.attr("href")
                                    ?.substringAfterLast("-")
                                    ?: "",
                                title = it.select("h3.film-name").text(),
                                quality = info.quality ?: "",
                                rating = info.rating,
                                poster = it.selectFirst("div.film-poster > img.film-poster-img")
                                    ?.attr("data-src")
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
                                } ?: listOf()
                            )
                        },
                )
            )

            State.SuccessLoading(categories)
        } catch (e: Exception) {
            State.FailedLoading(e)
        }
    }
}