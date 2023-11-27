package com.tanasi.streamflix.fragments.movies

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.utils.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MoviesViewModel : ViewModel() {

    private val _state = MutableLiveData<State>(State.Loading)
    val state: LiveData<State> = _state

    private var page = 1

    sealed class State {
        object Loading : State()
        object LoadingMore : State()
        data class SuccessLoading(val movies: List<Movie>, val hasMore: Boolean) : State()
        data class FailedLoading(val error: Exception) : State()
    }

    init {
        getMovies()
    }


    private fun getMovies() = viewModelScope.launch(Dispatchers.IO) {
        _state.postValue(State.Loading)

        try {
            val movies = UserPreferences.currentProvider!!.getMovies(page)

            _state.postValue(State.SuccessLoading(movies, true))
        } catch (e: Exception) {
            _state.postValue(State.FailedLoading(e))
        }
    }

    fun loadMoreMovies() = viewModelScope.launch(Dispatchers.IO) {
        val currentState = state.value
        if (currentState is State.SuccessLoading) {
            _state.postValue(State.LoadingMore)

            try {
                val movies = UserPreferences.currentProvider!!.getMovies(page + 1)

                _state.postValue(
                    State.SuccessLoading(
                        movies = currentState.movies + movies,
                        hasMore = movies.isNotEmpty(),
                    )
                ).run { page += 1 }
            } catch (e: Exception) {
                _state.postValue(State.FailedLoading(e))
            }
        }
    }
}