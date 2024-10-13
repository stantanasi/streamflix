package com.tanasi.streamflix.fragments.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.database.AppDatabase
import com.tanasi.streamflix.models.Genre
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.utils.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

class SearchViewModel(database: AppDatabase) : ViewModel() {

    private val _state = MutableStateFlow<State>(State.Searching)
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: Flow<State> = combine(
        _state,
        _state.transformLatest { state ->
            when (state) {
                is State.SuccessSearching -> {
                    val movies = state.results
                        .filterIsInstance<Movie>()
                    database.movieDao().getByIds(movies.map { it.id })
                        .collect { emit(it) }
                }
                else -> emit(emptyList<Movie>())
            }
        },
        _state.transformLatest { state ->
            when (state) {
                is State.SuccessSearching -> {
                    val tvShows = state.results
                        .filterIsInstance<TvShow>()
                    database.tvShowDao().getByIds(tvShows.map { it.id })
                        .collect { emit(it) }
                }
                else -> emit(emptyList<TvShow>())
            }
        },
    ) { state, moviesDb, tvShowsDb ->
        when (state) {
            is State.SuccessSearching -> {
                State.SuccessSearching(
                    results = state.results.map { item ->
                        when (item) {
                            is Movie -> moviesDb.find { it.id == item.id }
                                ?.takeIf { !item.isSame(it) }
                                ?.let { item.copy().merge(it) }
                                ?: item
                            is TvShow -> tvShowsDb.find { it.id == item.id }
                                ?.takeIf { !item.isSame(it) }
                                ?.let { item.copy().merge(it) }
                                ?: item
                            else -> item
                        }
                    },
                    hasMore = state.hasMore
                )
            }
            else -> state
        }
    }

    var query = ""
    private var page = 1

    sealed class State {
        data object Searching : State()
        data object SearchingMore : State()
        data class SuccessSearching(val results: List<AppAdapter.Item>, val hasMore: Boolean) : State()
        data class FailedSearching(val error: Exception) : State()
    }

    init {
        search(query)
    }


    fun search(query: String) = viewModelScope.launch(Dispatchers.IO) {
        _state.emit(State.Searching)

        try {
            val results = UserPreferences.currentProvider!!.search(query)
                .sortedBy {
                    when (it) {
                        is Genre -> it.name
                        else -> ""
                    }
                }

            this@SearchViewModel.query = query
            page = 1

            _state.emit(State.SuccessSearching(results, true))
        } catch (e: Exception) {
            Log.e("SearchViewModel", "search: ", e)
            _state.emit(State.FailedSearching(e))
        }
    }

    fun loadMore() = viewModelScope.launch(Dispatchers.IO) {
        val currentState = _state.first()
        if (currentState is State.SuccessSearching) {
            _state.emit(State.SearchingMore)

            try {
                val results = UserPreferences.currentProvider!!.search(query, page + 1)

                page += 1

                _state.emit(
                    State.SuccessSearching(
                        results = currentState.results + results,
                        hasMore = results.isNotEmpty(),
                    )
                )
            } catch (e: Exception) {
                Log.e("SearchViewModel", "loadMore: ", e)
                _state.emit(State.FailedSearching(e))
            }
        }
    }
}