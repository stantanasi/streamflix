package com.tanasi.streamflix.fragments.tv_shows

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.utils.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TvShowsViewModel : ViewModel() {

    private val _state = MutableLiveData<State>(State.Loading)
    val state: LiveData<State> = _state

    private var page = 1

    sealed class State {
        object Loading : State()
        data class SuccessLoading(val tvShows: List<TvShow>) : State()
        data class FailedLoading(val error: Exception) : State()
    }

    init {
        getTvShows()
    }


    private fun getTvShows() = viewModelScope.launch(Dispatchers.IO) {
        _state.postValue(State.Loading)

        try {
            val tvShows = UserPreferences.currentProvider!!.getTvShows(page)

            _state.postValue(State.SuccessLoading(tvShows))
        } catch (e: Exception) {
            _state.postValue(State.FailedLoading(e))
        }
    }
}