package com.tanasi.sflix.fragments.people

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tanasi.sflix.models.People
import com.tanasi.sflix.services.SflixService

class PeopleViewModel : ViewModel() {

    private val sflixService = SflixService.build()

    private val _state = MutableLiveData<State>(State.Loading)
    val state: LiveData<State> = _state

    sealed class State {
        object Loading : State()

        data class SuccessLoading(val people: People) : State()
        data class FailedLoading(val error: Exception) : State()
    }
}