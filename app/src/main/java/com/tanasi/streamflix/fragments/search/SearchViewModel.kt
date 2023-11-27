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
        object SearchingMore : State()
        data class SuccessSearching(val results: List<AppAdapter.Item>, val hasMore: Boolean) : State()
        data class FailedSearching(val error: Exception) : State()
    }

    init {
        search(query)
    }


    fun search(query: String) = viewModelScope.launch(Dispatchers.IO) {
        _state.postValue(State.Searching)

        try {
            val results = UserPreferences.currentProvider!!.search(query, page)

            this@SearchViewModel.query = query
            _state.postValue(State.SuccessSearching(results, true))
        } catch (e: Exception) {
            _state.postValue(State.FailedSearching(e))
        }
    }

    fun loadMore() = viewModelScope.launch(Dispatchers.IO) {
        val currentState = state.value
        if (currentState is State.SuccessSearching) {
            _state.postValue(State.SearchingMore)

            try {
                val results = UserPreferences.currentProvider!!.search(query, page + 1)

                _state.postValue(
                    State.SuccessSearching(
                        results = currentState.results + results,
                        hasMore = results.isNotEmpty(),
                    )
                ).run { page += 1 }
            } catch (e: Exception) {
                _state.postValue(State.FailedSearching(e))
            }
        }
    }
}