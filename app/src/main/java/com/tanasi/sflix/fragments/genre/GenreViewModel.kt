package com.tanasi.sflix.fragments.genre

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.sflix.models.Genre
import com.tanasi.sflix.utils.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GenreViewModel(id: String) : ViewModel() {

    private val _state = MutableLiveData<State>(State.Loading)
    val state: LiveData<State> = _state

    sealed class State {
        object Loading : State()
        data class SuccessLoading(val genre: Genre) : State()
        data class FailedLoading(val error: Exception) : State()
    }

    init {
        getGenre(id)
    }


    private fun getGenre(id: String) = viewModelScope.launch(Dispatchers.IO) {
        _state.postValue(State.Loading)

        try {
            val genre = UserPreferences.currentProvider.getGenre(id)

            _state.postValue(State.SuccessLoading(genre))
        } catch (e: Exception) {
            _state.postValue(State.FailedLoading(e))
        }
    }
}