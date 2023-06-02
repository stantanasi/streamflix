package com.tanasi.streamflix.fragments.season

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.streamflix.models.Episode
import com.tanasi.streamflix.utils.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SeasonViewModel(seasonId: String) : ViewModel() {

    private val _state = MutableLiveData<State>(State.LoadingEpisodes)
    val state: LiveData<State> = _state

    sealed class State {
        object LoadingEpisodes : State()
        data class SuccessLoadingEpisodes(val episodes: List<Episode>) : State()
        data class FailedLoadingEpisodes(val error: Exception) : State()
    }

    init {
        getSeasonEpisodes(seasonId)
    }


    private fun getSeasonEpisodes(seasonId: String) = viewModelScope.launch(Dispatchers.IO) {
        _state.postValue(State.LoadingEpisodes)

        try {
            val episodes = UserPreferences.currentProvider!!.getSeasonEpisodes(seasonId)

            _state.postValue(State.SuccessLoadingEpisodes(episodes))
        } catch (e: Exception) {
            _state.postValue(State.FailedLoadingEpisodes(e))
        }
    }
}