package com.tanasi.sflix.fragments.season

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.sflix.models.Episode
import com.tanasi.sflix.utils.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SeasonViewModel : ViewModel() {

    private val _state = MutableLiveData<State>(State.LoadingEpisodes)
    val state: LiveData<State> = _state

    sealed class State {
        object LoadingEpisodes : State()

        data class SuccessLoadingEpisodes(val episodes: List<Episode>) : State()
        data class FailedLoadingEpisodes(val error: Exception) : State()
    }


    fun getSeasonEpisodesById(seasonId: String) = viewModelScope.launch(Dispatchers.IO) {
        _state.postValue(State.LoadingEpisodes)

        try {
            val episodes = AppPreferences.currentProvider.getSeasonEpisodes(seasonId)

            _state.postValue(State.SuccessLoadingEpisodes(episodes))
        } catch (e: Exception) {
            _state.postValue(State.FailedLoadingEpisodes(e))
        }
    }
}