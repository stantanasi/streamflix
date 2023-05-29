package com.tanasi.sflix.fragments.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.sflix.adapters.SflixAdapter
import com.tanasi.sflix.utils.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {

    private val _state = MutableLiveData<State>(State.SuccessSearching(listOf()))
    val state: LiveData<State> = _state

    sealed class State {
        object Searching : State()
        data class SuccessSearching(val results: List<SflixAdapter.Item>) : State()
        data class FailedSearching(val error: Exception) : State()
    }

    init {
        search("")
    }


    fun search(query: String) = viewModelScope.launch(Dispatchers.IO) {
        _state.postValue(State.Searching)

        try {
            val results = UserPreferences.currentProvider.search(query)

            _state.postValue(State.SuccessSearching(results))
        } catch (e: Exception) {
            _state.postValue(State.FailedSearching(e))
        }
    }
}