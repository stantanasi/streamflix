package com.tanasi.sflix.fragments.tv_show

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.sflix.models.TvShow
import com.tanasi.sflix.utils.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TvShowViewModel : ViewModel() {

    private val _state = MutableLiveData<State>(State.Loading)
    val state: LiveData<State> = _state

    sealed class State {
        object Loading : State()

        data class SuccessLoading(val tvShow: TvShow) : State()
        data class FailedLoading(val error: Exception) : State()
    }


    fun getTvShowById(id: String) = viewModelScope.launch(Dispatchers.IO) {
        _state.postValue(State.Loading)

        try {
            val tvShow = AppPreferences.currentProvider.getTvShow(id)

            _state.postValue(State.SuccessLoading(tvShow))
        } catch (e: Exception) {
            _state.postValue(State.FailedLoading(e))
        }
    }
}