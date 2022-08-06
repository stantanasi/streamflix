package com.tanasi.sflix.fragments.movies

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tanasi.sflix.models.Movie
import com.tanasi.sflix.services.SflixService

class MoviesViewModel : ViewModel() {

    private val sflixService = SflixService.build()

    private val _state = MutableLiveData<State>(State.Searching)
    val state: LiveData<State> = _state

    sealed class State {
        object Searching : State()

        data class SuccessLoading(val movies: List<Movie>) : State()
        data class FailedLoading(val error: Exception) : State()
    }
}