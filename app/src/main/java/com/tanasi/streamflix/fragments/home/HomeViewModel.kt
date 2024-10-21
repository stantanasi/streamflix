package com.tanasi.streamflix.fragments.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.streamflix.database.AppDatabase
import com.tanasi.streamflix.models.Category
import com.tanasi.streamflix.models.Episode
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.utils.UserPreferences
import com.tanasi.streamflix.utils.combine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

class HomeViewModel(database: AppDatabase) : ViewModel() {

    private val _state = MutableStateFlow<State>(State.Loading)
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: Flow<State> = combine(
        _state,
        combine(
            database.movieDao().getWatchingMovies(),
            database.episodeDao().getWatchingEpisodes(),
            database.episodeDao().getNextEpisodesToWatch(),
        ) { watchingMovies, watchingEpisodes, watchNextEpisodes ->
            watchingMovies + watchingEpisodes.onEach { episode ->
                episode.tvShow = episode.tvShow?.let { database.tvShowDao().getById(it.id) }
                episode.season = episode.season?.let { database.seasonDao().getById(it.id) }
            } + watchNextEpisodes.onEach { episode ->
                episode.tvShow = episode.tvShow?.let { database.tvShowDao().getById(it.id) }
                episode.season = episode.season?.let { database.seasonDao().getById(it.id) }
            }
        },
        database.movieDao().getFavorites(),
        database.tvShowDao().getFavorites(),
        _state.transformLatest { state ->
            when (state) {
                is State.SuccessLoading -> {
                    val movies = state.categories
                        .flatMap { it.list }
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
                    val tvShows = state.categories
                        .flatMap { it.list }
                        .filterIsInstance<TvShow>()
                    database.tvShowDao().getByIds(tvShows.map { it.id })
                        .collect { emit(it) }
                }
                else -> emit(emptyList<TvShow>())
            }
        },
    ) { state, continueWatching, favoritesMovies, favoriteTvShows, moviesDb, tvShowsDb ->
        when (state) {
            is State.SuccessLoading -> {
                val categories = listOfNotNull(
                    state.categories
                        .find { it.name == Category.FEATURED }
                        ?.let { category ->
                            category.copy(
                                list = category.list.map { item ->
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
                                }
                            )
                        },

                    Category(
                        name = Category.CONTINUE_WATCHING,
                        list = continueWatching
                            .sortedByDescending {
                                it.watchHistory?.lastEngagementTimeUtcMillis
                                    ?: it.watchedDate?.timeInMillis
                            }
                            .distinctBy {
                                when (it) {
                                    is Episode -> it.tvShow?.id
                                    else -> false
                                }
                            },
                    ),

                    Category(
                        name = Category.FAVORITE_MOVIES,
                        list = favoritesMovies
                            .reversed(),
                    ),

                    Category(
                        name = Category.FAVORITE_TV_SHOWS,
                        list = favoriteTvShows
                            .reversed(),
                    ),
                ) + state.categories
                    .filter { it.name != Category.FEATURED }
                    .map { category ->
                        category.copy(
                            list = category.list.map { item ->
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
                            }
                        )
                    }

                State.SuccessLoading(categories)
            }
            else -> state
        }
    }

    sealed class State {
        data object Loading : State()
        data class SuccessLoading(val categories: List<Category>) : State()
        data class FailedLoading(val error: Exception) : State()
    }

    init {
        getHome()
    }


    fun getHome() = viewModelScope.launch(Dispatchers.IO) {
        _state.emit(State.Loading)

        try {
            val categories = UserPreferences.currentProvider!!.getHome()

            _state.emit(State.SuccessLoading(categories))
        } catch (e: Exception) {
            Log.e("HomeViewModel", "getHome: ", e)
            _state.emit(State.FailedLoading(e))
        }
    }
}