package com.tanasi.streamflix.fragments.tv_show

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.streamflix.database.AppDatabase
import com.tanasi.streamflix.models.Episode
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.models.Season
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

class TvShowViewModel(id: String, private val database: AppDatabase) : ViewModel() {

    private val _state = MutableStateFlow<State>(State.Loading)
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: Flow<State> = combine(
        _state.transformLatest { state ->
            when (state) {
                is State.SuccessLoading -> {
                    val episodes = database.episodeDao().getByTvShowIdAsFlow(id).first()
                    state.tvShow.seasons.onEach { season ->
                        season.episodes = episodes.filter { it.season?.id == season.id }
                    }

                    if (episodes.isEmpty() && state.tvShow.seasons.isNotEmpty()) {
                        val firstSeason = state.tvShow.seasons.firstOrNull { it.number != 0 }
                            ?: state.tvShow.seasons.first()
                        getSeason(TvShow(id, ""), firstSeason)
                    } else {
                        val season = state.tvShow.seasons.let { seasons ->
                            seasons
                                .lastOrNull { season ->
                                    season.episodes.lastOrNull()?.isWatched == true ||
                                            season.episodes.any { it.isWatched }
                                }?.let { season ->
                                    if (season.episodes.lastOrNull()?.isWatched == true) {
                                        val next = seasons.getOrNull(seasons.indexOf(season) + 1)
                                        next ?: season
                                    } else season
                                }
                                ?: seasons.firstOrNull { season ->
                                    season.episodes.isEmpty() ||
                                            season.episodes.lastOrNull()?.isWatched == false
                                }
                        }

                        val episodeIndex = episodes
                            .filter { it.watchHistory != null }
                            .sortedByDescending { it.watchHistory?.lastEngagementTimeUtcMillis }
                            .indexOfFirst { it.watchHistory != null }.takeIf { it != -1 }
                            ?: season?.episodes?.indexOfLast { it.isWatched }
                                ?.takeIf { it != -1 && it + 1 < episodes.size }
                                ?.let { it + 1 }

                        if (
                            episodeIndex == null &&
                            season != null &&
                            (season.episodes.isEmpty() || state.tvShow.seasons.lastOrNull() == season)
                        ) {
                            getSeason(state.tvShow, season)
                        }
                    }
                }
                else -> {}
            }
            emit(state)
        },
        database.tvShowDao().getByIdAsFlow(id),
        database.episodeDao().getByTvShowIdAsFlow(id),
        _state.transformLatest { state ->
            when (state) {
                is State.SuccessLoading -> {
                    val movies = state.tvShow.recommendations
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
                    val tvShows = state.tvShow.recommendations
                        .filterIsInstance<TvShow>()
                    database.tvShowDao().getByIds(tvShows.map { it.id })
                        .collect { emit(it) }
                }
                else -> emit(emptyList<TvShow>())
            }
        },
    ) { state, tvShowDb, episodesDb, moviesDb, tvShowsDb ->
        when (state) {
            is State.SuccessLoading -> {
                State.SuccessLoading(
                    tvShow = state.tvShow.copy(
                        seasons = (state.tvShow.seasons
                            .takeIf { seasons -> seasons.flatMap { it.episodes } != episodesDb }
                            ?.map { season ->
                                season.copy(
                                    episodes = episodesDb
                                        .filter { it.season?.id == season.id }
                                        .onEach { it.season = season }
                                )
                            }
                            ?: state.tvShow.seasons)
                            .sortedWith { season1, season2 ->
                                when {
                                    season1.number == 0 && season2.number == 0 -> 0
                                    season1.number == 0 -> 1
                                    season2.number == 0 -> -1
                                    else -> season1.number.compareTo(season2.number)
                                }
                            },
                        recommendations = state.tvShow.recommendations.map { show ->
                            when (show) {
                                is Movie -> moviesDb.find { it.id == show.id }
                                    ?.takeIf { !show.isSame(it) }
                                    ?.let { show.copy().merge(it) }
                                    ?: show
                                is TvShow -> tvShowsDb.find { it.id == show.id }
                                    ?.takeIf { !show.isSame(it) }
                                    ?.let { show.copy().merge(it) }
                                    ?: show
                            }
                        },
                    ).also { tvShow ->
                        tvShowDb?.let { tvShow.merge(it) }
                    }
                )
            }
            else -> state
        }
    }

    sealed class State {
        data object Loading : State()
        data class SuccessLoading(val tvShow: TvShow) : State()
        data class FailedLoading(val error: Exception) : State()
    }

    private val _seasonState = MutableStateFlow<SeasonState>(SeasonState.Loading)

    sealed class SeasonState {
        data object Loading :  SeasonState()
        data class SuccessLoading(
            val tvShow: TvShow,
            val season: Season,
            val episodes: List<Episode>,
        ) : SeasonState()
        data class FailedLoading(val error: Exception) : SeasonState()
    }

    init {
        getTvShow(id)
    }


    fun getTvShow(id: String) = viewModelScope.launch(Dispatchers.IO) {
        _state.emit(State.Loading)

        try {
            val tvShow = UserPreferences.currentProvider!!.getTvShow(id)

            database.tvShowDao().getByIdAsFlow(tvShow.id).first()?.let { tvShowDb ->
                tvShow.merge(tvShowDb)
            }
            database.tvShowDao().insert(tvShow)

            val tvShowCopy = tvShow.copy()
            tvShow.seasons.forEach { season ->
                season.tvShow = tvShowCopy
            }
            database.seasonDao().insertAll(tvShow.seasons)

            _state.emit(State.SuccessLoading(tvShow))
        } catch (e: Exception) {
            Log.e("TvShowViewModel", "getTvShow: ", e)
            _state.emit(State.FailedLoading(e))
        }
    }

    private fun getSeason(tvShow: TvShow, season: Season) = viewModelScope.launch(Dispatchers.IO) {
        _seasonState.emit(SeasonState.Loading)

        try {
            val episodes = UserPreferences.currentProvider!!.getEpisodesBySeason(season.id)


            database.episodeDao().getByIdsAsFlow(episodes.map { it.id }).first()
                .forEach { episodeDb ->
                    episodes.find { it.id == episodeDb.id }
                        ?.merge(episodeDb)
                }
            episodes.onEach { episode ->
                episode.tvShow = tvShow
                episode.season = season
            }
            database.episodeDao().insertAll(episodes)

            _seasonState.emit(SeasonState.SuccessLoading(tvShow, season, episodes))
        } catch (e: Exception) {
            Log.e("TvShowViewModel", "getSeason: ", e)
            _seasonState.emit(SeasonState.FailedLoading(e))
        }
    }
}