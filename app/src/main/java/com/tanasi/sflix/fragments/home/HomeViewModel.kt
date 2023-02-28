package com.tanasi.sflix.fragments.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.sflix.models.Category
import com.tanasi.sflix.utils.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val _state = MutableLiveData<State>(State.Loading)
    val state: LiveData<State> = _state

    sealed class State {
        object Loading : State()

        data class SuccessLoading(val categories: List<Category>) : State()
        data class FailedLoading(val error: Exception) : State()
    }

    init {
        getHome()
    }


    private fun getHome() = viewModelScope.launch(Dispatchers.IO) {
        _state.postValue(State.Loading)

        try {
            val categories = AppPreferences.currentProvider.getHome()

            _state.postValue(State.SuccessLoading(categories))
        } catch (e: Exception) {
            _state.postValue(State.FailedLoading(e))
        }
    }
}