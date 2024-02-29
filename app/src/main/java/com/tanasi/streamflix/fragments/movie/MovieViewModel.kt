package com.tanasi.streamflix.fragments.movie

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.utils.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MovieViewModel(id: String) : ViewModel() {

    private val _state = MutableLiveData<State>(State.Loading)
    val state: LiveData<State> = _state

    sealed class State {
        data object Loading : State()
        data class SuccessLoading(val movie: Movie) : State()
        data class FailedLoading(val error: Exception) : State()
    }

    init {
        getMovie(id)
    }


    fun getMovie(id: String) = viewModelScope.launch(Dispatchers.IO) {
        _state.postValue(State.Loading)

        try {
            val movie = UserPreferences.currentProvider!!.getMovie(id)

            _state.postValue(State.SuccessLoading(movie))
        } catch (e: Exception) {
            Log.e("MovieViewModel", "getMovie: ", e)
            _state.postValue(State.FailedLoading(e))
        }
    }
}