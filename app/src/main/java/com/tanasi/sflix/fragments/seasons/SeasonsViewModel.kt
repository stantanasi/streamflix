package com.tanasi.sflix.fragments.seasons

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.sflix.models.Episode
import com.tanasi.sflix.models.Season
import com.tanasi.sflix.services.SflixService
import kotlinx.coroutines.launch

class SeasonsViewModel : ViewModel() {

    private val sflixService = SflixService.build()

    private val _state = MutableLiveData<State>(State.LoadingSeasons)
    val state: LiveData<State> = _state

    sealed class State {
        object LoadingSeasons : State()
        data class SuccessLoadingSeasons(val seasons: List<Season>) : State()
        data class FailedLoadingSeasons(val error: Exception) : State()

        object LoadingEpisodes : State()
        data class SuccessLoadingEpisodes(
            val seasonId: String,
            val episodes: List<Episode>,
        ) : State()
        data class FailedLoadingEpisodes(val error: Exception) : State()
    }


    fun getSeasons(tvShowId: String) = viewModelScope.launch {
        _state.value = State.LoadingSeasons

        _state.value = try {
            State.SuccessLoadingSeasons(
                seasons = sflixService.fetchTvShowSeasons(tvShowId)
                    .select("div.dropdown-menu.dropdown-menu-model > a")
                    .mapIndexed { seasonNumber, seasonElement ->
                        val seasonId = seasonElement.attr("data-id")
                        Season(
                            id = seasonId,
                            number = seasonNumber + 1,
                            title = seasonElement.text(),
                        )
                    }
            )
        } catch (e: Exception) {
            State.FailedLoadingSeasons(e)
        }
    }
}