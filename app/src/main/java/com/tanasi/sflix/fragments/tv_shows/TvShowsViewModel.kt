package com.tanasi.sflix.fragments.tv_shows

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tanasi.sflix.models.TvShow
import com.tanasi.sflix.services.SflixService

class TvShowsViewModel : ViewModel() {

    private val sflixService = SflixService.build()

    private val _state = MutableLiveData<State>(State.Loading)
    val state: LiveData<State> = _state

    sealed class State {
        object Loading : State()

        data class SuccessLoading(val tvShows: List<TvShow>) : State()
        data class FailedLoading(val error: Exception) : State()
    }
}