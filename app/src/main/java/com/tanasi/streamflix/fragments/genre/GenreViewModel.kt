package com.tanasi.streamflix.fragments.genre

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.streamflix.models.Genre
import com.tanasi.streamflix.utils.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GenreViewModel(private val id: String) : ViewModel() {

    private val _state = MutableLiveData<State>(State.Loading)
    val state: LiveData<State> = _state

    private var page = 1

    sealed class State {
        object Loading : State()
        object LoadingMore : State()
        data class SuccessLoading(val genre: Genre, val hasMore: Boolean) : State()
        data class FailedLoading(val error: Exception) : State()
    }

    init {
        getGenre(id)
    }


    private fun getGenre(id: String) = viewModelScope.launch(Dispatchers.IO) {
        _state.postValue(State.Loading)

        try {
            val genre = UserPreferences.currentProvider!!.getGenre(id)

            page = 1

            _state.postValue(State.SuccessLoading(genre, true))
        } catch (e: Exception) {
            _state.postValue(State.FailedLoading(e))
        }
    }

    fun loadMoreGenreShows() = viewModelScope.launch(Dispatchers.IO) {
        val currentState = state.value
        if (currentState is State.SuccessLoading) {
            _state.postValue(State.LoadingMore)

            try {
                val genre = UserPreferences.currentProvider!!.getGenre(id, page + 1)

                page += 1

                _state.postValue(
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
                _state.postValue(State.FailedLoading(e))
            }
        }
    }
}