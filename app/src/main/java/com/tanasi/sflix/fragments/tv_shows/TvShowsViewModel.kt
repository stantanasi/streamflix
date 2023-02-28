package com.tanasi.sflix.fragments.tv_shows

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.sflix.models.TvShow
import com.tanasi.sflix.utils.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TvShowsViewModel : ViewModel() {

    private val _state = MutableLiveData<State>(State.Loading)
    val state: LiveData<State> = _state

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
            val tvShows = AppPreferences.currentProvider.getTvShows()

            _state.postValue(State.SuccessLoading(tvShows))
        } catch (e: Exception) {
            _state.postValue(State.FailedLoading(e))
        }
    }
}