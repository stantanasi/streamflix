package com.tanasi.sflix.fragments.tv_show

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tanasi.sflix.models.TvShow
import com.tanasi.sflix.services.SflixService

class TvShowViewModel : ViewModel() {

    private val sflixService = SflixService.build()

    private val _state: MutableLiveData<State> = MutableLiveData(State.Loading)
    val state: LiveData<State> = _state

    sealed class State {
        object Loading : State()
        data class SuccessLoading(val tvShow: TvShow) : State()
    }
}