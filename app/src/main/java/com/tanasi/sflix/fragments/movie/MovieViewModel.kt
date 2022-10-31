package com.tanasi.sflix.fragments.movie

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.sflix.models.Movie
import com.tanasi.sflix.providers.SflixProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MovieViewModel : ViewModel() {

    private val _state = MutableLiveData<State>(State.Loading)
    val state: LiveData<State> = _state

    sealed class State {
        object Loading : State()

        data class SuccessLoading(val movie: Movie) : State()
        data class FailedLoading(val error: Exception) : State()
    }


    fun getMovieById(id: String) = viewModelScope.launch(Dispatchers.IO) {
        _state.postValue(State.Loading)

        try {
            val movie = SflixProvider.getMovie(id)

            _state.postValue(State.SuccessLoading(movie))
        } catch (e: Exception) {
            _state.postValue(State.FailedLoading(e))
        }
    }
}