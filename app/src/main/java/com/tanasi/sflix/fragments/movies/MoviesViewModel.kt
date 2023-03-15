package com.tanasi.sflix.fragments.movies

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.sflix.models.Movie
import com.tanasi.sflix.utils.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MoviesViewModel : ViewModel() {

    private val _state = MutableLiveData<State>(State.Loading)
    val state: LiveData<State> = _state

    sealed class State {
        object Loading : State()
        data class SuccessLoading(val movies: List<Movie>) : State()
        data class FailedLoading(val error: Exception) : State()
    }

    init {
        getMovies()
    }


    private fun getMovies() = viewModelScope.launch(Dispatchers.IO) {
        _state.postValue(State.Loading)

        try {
            val movies = AppPreferences.currentProvider.getMovies()

            _state.postValue(State.SuccessLoading(movies))
        } catch (e: Exception) {
            _state.postValue(State.FailedLoading(e))
        }
    }
}