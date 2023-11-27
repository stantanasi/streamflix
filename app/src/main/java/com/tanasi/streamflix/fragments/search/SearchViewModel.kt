package com.tanasi.streamflix.fragments.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.utils.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {

    private val _state = MutableLiveData<State>(State.Searching)
    val state: LiveData<State> = _state

    var query = ""
    private var page = 1

    sealed class State {
        object Searching : State()
        data class SuccessSearching(val results: List<AppAdapter.Item>) : State()
        data class FailedSearching(val error: Exception) : State()
    }

    init {
        search(query)
    }


    fun search(query: String) = viewModelScope.launch(Dispatchers.IO) {
        _state.postValue(State.Searching)

        try {
            val results = UserPreferences.currentProvider!!.search(query, page)

            _state.postValue(State.SuccessSearching(results))
                .run { this@SearchViewModel.query = query }
        } catch (e: Exception) {
            _state.postValue(State.FailedSearching(e))
        }
    }
}