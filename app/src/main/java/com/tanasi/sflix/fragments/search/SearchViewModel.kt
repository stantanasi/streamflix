package com.tanasi.sflix.fragments.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tanasi.sflix.services.SflixService

class SearchViewModel : ViewModel() {

    private val sflixService = SflixService.build()

    private val _state: MutableLiveData<State> = MutableLiveData(State.Searching)
    val state: LiveData<State> = _state

    sealed class State {
        object Searching : State()
    }
}