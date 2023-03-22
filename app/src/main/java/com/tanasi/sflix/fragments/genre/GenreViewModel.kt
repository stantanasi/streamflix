package com.tanasi.sflix.fragments.genre

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tanasi.sflix.models.Genre

class GenreViewModel : ViewModel() {

    private val _state = MutableLiveData<State>(State.Loading)
    val state: LiveData<State> = _state

    sealed class State {
        object Loading : State()
        data class SuccessLoading(val genre: Genre) : State()
        data class FailedLoading(val error: Exception) : State()
    }
}