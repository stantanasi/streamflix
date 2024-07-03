package com.tanasi.streamflix.fragments.people

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.streamflix.database.AppDatabase
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.models.People
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

class PeopleViewModel(private val id: String, database: AppDatabase) : ViewModel() {

    private val _state = MutableStateFlow<State>(State.Loading)
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: Flow<State> = combine(
        _state,
        _state.transformLatest { state ->
            when (state) {
                is State.SuccessLoading -> {
                    val movies = state.people.filmography
                        .filterIsInstance<Movie>()
                    database.movieDao().getByIds(movies.map { it.id }).collect { emit(it) }
                }
                else -> emit(emptyList<Movie>())
            }
        },
        _state.transformLatest { state ->
            when (state) {
                is State.SuccessLoading -> {
                    val tvShows = state.people.filmography
                        .filterIsInstance<TvShow>()
                    database.tvShowDao().getByIds(tvShows.map { it.id }).collect { emit(it) }
                }
                else -> emit(emptyList<TvShow>())
            }
        },
    ) { state, moviesDb, tvShowsDb ->
        when (state) {
            is State.SuccessLoading -> {
                State.SuccessLoading(
                    people = state.people.copy(
                        filmography = state.people.filmography.map { item ->
                            when (item) {
                                is Movie -> moviesDb.find { it.id == item.id }
                                    ?.takeIf { !item.isSame(it) }
                                    ?.let { item.copy().merge(it) }
                                    ?: item
                                is TvShow -> tvShowsDb.find { it.id == item.id }
                                    ?.takeIf { !item.isSame(it) }
                                    ?.let { item.copy().merge(it) }
                                    ?: item
                            }
                        }
                    ),
                    hasMore = state.hasMore
                )
            }
            else -> state
        }
    }

    private var page = 1

    sealed class State {
        data object Loading : State()
        data object LoadingMore : State()
        data class SuccessLoading(val people: People, val hasMore: Boolean) : State()
        data class FailedLoading(val error: Exception) : State()
    }

    init {
        getPeople(id)
    }


    fun getPeople(id: String) = viewModelScope.launch(Dispatchers.IO) {
        _state.emit(State.Loading)

        try {
            val people = UserPreferences.currentProvider!!.getPeople(id)

            page = 1

            _state.emit(State.SuccessLoading(people, true))
        } catch (e: Exception) {
            Log.e("PeopleViewModel", "getPeople: ", e)
            _state.emit(State.FailedLoading(e))
        }
    }

    fun loadMorePeopleFilmography() = viewModelScope.launch(Dispatchers.IO) {
        val currentState = _state.first()
        if (currentState is State.SuccessLoading) {
            _state.emit(State.LoadingMore)

            try {
                val people = UserPreferences.currentProvider!!.getPeople(id, page + 1)

                page += 1

                _state.emit(
                    State.SuccessLoading(
                        people = currentState.people.copy(
                            filmography = currentState.people.filmography + people.filmography
                        ),
                        hasMore = people.filmography.isNotEmpty(),
                    )
                )
            } catch (e: Exception) {
                Log.e("PeopleViewModel", "loadMorePeopleFilmography: ", e)
                _state.emit(State.FailedLoading(e))
            }
        }
    }
}