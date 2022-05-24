package com.tanasi.sflix.fragments.tv_show

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.sflix.models.Episode
import com.tanasi.sflix.models.Season
import com.tanasi.sflix.models.TvShow
import com.tanasi.sflix.services.SflixService
import kotlinx.coroutines.launch

class TvShowViewModel : ViewModel() {

    private val sflixService = SflixService.build()

    private val _state: MutableLiveData<State> = MutableLiveData(State.Loading)
    val state: LiveData<State> = _state

    sealed class State {
        object Loading : State()
        data class SuccessLoading(val tvShow: TvShow) : State()
    }

    fun fetchTvShow(id: String) = viewModelScope.launch {
        _state.value = State.Loading

        val document = sflixService.fetchTvShow(id)

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
            TvShow(
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

                seasons = sflixService.fetchTvShowSeasons(id)
                    .select("div.dropdown-menu.dropdown-menu-model > a")
                    .mapIndexed { seasonNumber, seasonElement ->
                        val seasonId = seasonElement.attr("data-id")
                        Season(
                            id = seasonId,
                            number = seasonNumber + 1,
                            title = seasonElement.text(),

                            episodes = sflixService.fetchSeasonEpisode(seasonId)
                                .select("div.flw-item.film_single-item.episode-item.eps-item")
                                .mapIndexed { episodeNumber, episodeElement ->
                                    Episode(
                                        id = episodeElement.attr("data-id"),
                                        number = episodeElement
                                            .selectFirst("div.episode-number")
                                            ?.text()
                                            ?.substringAfter("Episode ")
                                            ?.substringBefore(":")
                                            ?.toIntOrNull()
                                            ?: episodeNumber,
                                        title = episodeElement
                                            .selectFirst("h3.film-name")
                                            ?.text()
                                            ?: "",
                                        poster = episodeElement.selectFirst("img")
                                            ?.attr("src") ?: ""
                                    )
                                }
                        )
                    },
            )
        )
    }
}