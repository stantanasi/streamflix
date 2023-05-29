package com.tanasi.sflix.fragments.movie

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.sflix.models.Movie
import com.tanasi.sflix.utils.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MovieViewModel(id: String) : ViewModel() {

    private val _state = MutableLiveData<State>(State.Loading)
    val state: LiveData<State> = _state

    sealed class State {
        object Loading : State()
        data class SuccessLoading(val movie: Movie) : State()
        data class FailedLoading(val error: Exception) : State()
    }

    init {
        getMovie(id)
    }


    private fun getMovie(id: String) = viewModelScope.launch(Dispatchers.IO) {
        _state.postValue(State.Loading)

        try {
            val movie = UserPreferences.currentProvider.getMovie(id)

            _state.postValue(State.SuccessLoading(movie))
        } catch (e: Exception) {
            _state.postValue(State.FailedLoading(e))
        }
    }
}