package com.tanasi.streamflix.fragments.season

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.streamflix.database.AppDatabase
import com.tanasi.streamflix.models.Episode
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

class SeasonViewModel(
    seasonId: String,
    private val tvShowId: String,
    private val database: AppDatabase,
) : ViewModel() {

    private val _state = MutableStateFlow<State>(State.LoadingEpisodes)
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: Flow<State> = combine(
        _state,
        _state.transformLatest { state ->
            when (state) {
                is State.SuccessLoadingEpisodes -> {
                    database.episodeDao().getByIdsAsFlow(state.episodes.map { it.id })
                        .collect { emit(it) }
                }
                else -> emit(emptyList<Episode>())
            }
        },
        database.tvShowDao().getByIdAsFlow(tvShowId),
        database.seasonDao().getByIdAsFlow(seasonId),
    ) { state, episodesDb, tvShow, season ->
        when (state) {
            is State.SuccessLoadingEpisodes -> {
                State.SuccessLoadingEpisodes(
                    episodes = state.episodes.map { episode ->
                        episodesDb.find { it.id == episode.id }
                            ?.takeIf { !episode.isSame(it) }
                            ?.let { episode.copy().merge(it) }
                            ?: episode
                    }.onEach { episode ->
                        episode.tvShow = tvShow
                        episode.season = season
                    }
                )
            }
            else -> state
        }
    }

    sealed class State {
        data object LoadingEpisodes : State()
        data class SuccessLoadingEpisodes(val episodes: List<Episode>) : State()
        data class FailedLoadingEpisodes(val error: Exception) : State()
    }

    init {
        getSeasonEpisodes(seasonId)
    }


    fun getSeasonEpisodes(seasonId: String) = viewModelScope.launch(Dispatchers.IO) {
        _state.emit(State.LoadingEpisodes)

        try {
            val episodes = UserPreferences.currentProvider!!.getEpisodesBySeason(seasonId)

            database.episodeDao().getByIdsAsFlow(episodes.map { it.id }).first()
                .forEach { episodeDb ->
                    episodes.find { it.id == episodeDb.id }
                        ?.merge(episodeDb)
                }

            val tvShow = TvShow(tvShowId)
            val season = Season(seasonId)
            episodes.forEach { episode ->
                episode.tvShow = tvShow
                episode.season = season
            }

            database.episodeDao().insertAll(episodes)

            _state.emit(State.SuccessLoadingEpisodes(episodes))
        } catch (e: Exception) {
            Log.e("SeasonViewModel", "getSeasonEpisodes: ", e)
            _state.emit(State.FailedLoadingEpisodes(e))
        }
    }
}