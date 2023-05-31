package com.tanasi.streamflix.fragments.tv_show

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    init {
        getTvShow(id)
    }


    private fun getTvShow(id: String) = viewModelScope.launch(Dispatchers.IO) {
        _state.postValue(State.Loading)

        try {
            val tvShow = UserPreferences.currentProvider.getTvShow(id)

            _state.postValue(State.SuccessLoading(tvShow))
        } catch (e: Exception) {
            _state.postValue(State.FailedLoading(e))
        }
    }
}