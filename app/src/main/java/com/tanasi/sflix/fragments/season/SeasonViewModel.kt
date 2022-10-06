package com.tanasi.sflix.fragments.season

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.sflix.models.Episode
import com.tanasi.sflix.services.SflixService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SeasonViewModel : ViewModel() {

    private val sflixService = SflixService.build()

    private val _state = MutableLiveData<State>(State.LoadingEpisodes)
    val state: LiveData<State> = _state

    sealed class State {
        object LoadingEpisodes : State()
        data class SuccessLoadingEpisodes(
            val seasonId: String,
            val episodes: List<Episode>,
        ) : State()
        data class FailedLoadingEpisodes(val error: Exception) : State()
    }


    fun getSeasonEpisodesById(seasonId: String) = viewModelScope.launch(Dispatchers.IO) {
        _state.postValue(State.LoadingEpisodes)

        try {
            val document = sflixService.getSeasonEpisodesById(seasonId)

            val episodes = document
                .select("div.flw-item.film_single-item.episode-item.eps-item")
                .mapIndexed { episodeNumber, episodeElement ->
                    val episodeId = episodeElement.attr("data-id")
                    Episode(
                        id = episodeId,
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
                            ?.attr("src") ?: "",
                    )
                }

            _state.postValue(State.SuccessLoadingEpisodes(seasonId, episodes))
        } catch (e: Exception) {
            _state.postValue(State.FailedLoadingEpisodes(e))
        }
    }
}