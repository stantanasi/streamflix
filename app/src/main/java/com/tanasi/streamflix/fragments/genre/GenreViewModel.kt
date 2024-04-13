package com.tanasi.streamflix.fragments.genre

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

class GenreViewModel(private val id: String, database: AppDatabase) : ViewModel() {

    private val _state = MutableStateFlow<State>(State.Loading)
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: Flow<State> = combine(
        _state,
        _state.transformLatest { state ->
            when (state) {
                is State.SuccessLoading -> {
                    val movies = state.genre.shows
                        .filterIsInstance<Movie>()
                    database.movieDao().getByIds(movies.map { it.id })
                        .collect { emit(it) }
                }
                else -> emit(emptyList<Movie>())
            }
        },
        _state.transformLatest { state ->
            when (state) {
                is State.SuccessLoading -> {
                    val tvShows = state.genre.shows
                        .filterIsInstance<TvShow>()
                    database.tvShowDao().getByIds(tvShows.map { it.id })
                        .collect { emit(it) }
                }
                else -> emit(emptyList<TvShow>())
            }
        },
    ) { state, moviesDb, tvShowsDb ->
        when (state) {
            is State.SuccessLoading -> {
                State.SuccessLoading(
                    genre = state.genre.copy(
                        shows = state.genre.shows.map { item ->
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
        data class SuccessLoading(val genre: Genre, val hasMore: Boolean) : State()
        data class FailedLoading(val error: Exception) : State()
    }

    init {
        getGenre(id)
    }


    fun getGenre(id: String) = viewModelScope.launch(Dispatchers.IO) {
        _state.emit(State.Loading)

        try {
            val genre = UserPreferences.currentProvider!!.getGenre(id)

            page = 1

            _state.emit(State.SuccessLoading(genre, true))
        } catch (e: Exception) {
            Log.e("GenreViewModel", "getGenre: ", e)
            _state.emit(State.FailedLoading(e))
        }
    }

    fun loadMoreGenreShows() = viewModelScope.launch(Dispatchers.IO) {
        val currentState = _state.first()
        if (currentState is State.SuccessLoading) {
            _state.emit(State.LoadingMore)

            try {
                val genre = UserPreferences.currentProvider!!.getGenre(id, page + 1)

                page += 1

                _state.emit(
                    State.SuccessLoading(
                        genre = Genre(
                            id = genre.id,
                            name = genre.name,

                            shows = currentState.genre.shows + genre.shows,
                        ),
                        hasMore = genre.shows.isNotEmpty(),
                    )
                )
            } catch (e: Exception) {
                Log.e("GenreViewModel", "loadMoreGenreShows: ", e)
                _state.emit(State.FailedLoading(e))
            }
        }
    }
}