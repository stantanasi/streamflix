package com.tanasi.streamflix.fragments.tv_show

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.streamflix.models.Episode
import com.tanasi.streamflix.models.Season
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.utils.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TvShowViewModel(id: String) : ViewModel() {

    private val _state = MutableLiveData<State>(State.Loading)
    val state: LiveData<State> = _state

    sealed class State {
        object Loading : State()
        data class SuccessLoading(val tvShow: TvShow) : State()
        data class FailedLoading(val error: Exception) : State()
    }

    private val _seasonState = MutableLiveData<SeasonState>(SeasonState.Loading)
    val seasonState: LiveData<SeasonState> = _seasonState

    sealed class SeasonState {
        object Loading :  SeasonState()
        data class SuccessLoading(
            val tvShow: TvShow,
            val season: Season,
            val episodes: List<Episode>,
        ) : SeasonState()
        data class FailedLoading(val error: Exception) : SeasonState()
    }

    init {
        getTvShow(id)
    }


    fun getTvShow(id: String) = viewModelScope.launch(Dispatchers.IO) {
        _state.postValue(State.Loading)

        try {
            val tvShow = UserPreferences.currentProvider!!.getTvShow(id)

            _state.postValue(State.SuccessLoading(tvShow))
        } catch (e: Exception) {
            Log.e("TvShowViewModel", "getTvShow: ", e)
            _state.postValue(State.FailedLoading(e))
        }
    }

    fun getSeason(tvShow: TvShow, season: Season) = viewModelScope.launch(Dispatchers.IO) {
        _seasonState.postValue(SeasonState.Loading)

        try {
            val episodes = UserPreferences.currentProvider!!.getEpisodesBySeason(season.id)

            _seasonState.postValue(SeasonState.SuccessLoading(tvShow, season, episodes))
        } catch (e: Exception) {
            Log.e("TvShowViewModel", "getFirstSeason: ", e)
            _seasonState.postValue(SeasonState.FailedLoading(e))
        }
    }
}