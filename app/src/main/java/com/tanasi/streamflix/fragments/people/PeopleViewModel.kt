package com.tanasi.streamflix.fragments.people

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.streamflix.models.People
import com.tanasi.streamflix.utils.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PeopleViewModel(private val id: String) : ViewModel() {

    private val _state = MutableLiveData<State>(State.Loading)
    val state: LiveData<State> = _state

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
        _state.postValue(State.Loading)

        try {
            val people = UserPreferences.currentProvider!!.getPeople(id)

            page = 1

            _state.postValue(State.SuccessLoading(people, true))
        } catch (e: Exception) {
            Log.e("PeopleViewModel", "getPeople: ", e)
            _state.postValue(State.FailedLoading(e))
        }
    }

    fun loadMorePeopleFilmography() = viewModelScope.launch(Dispatchers.IO) {
        val currentState = state.value
        if (currentState is State.SuccessLoading) {
            _state.postValue(State.LoadingMore)

            try {
                val people = UserPreferences.currentProvider!!.getPeople(id, page + 1)

                page += 1

                _state.postValue(
                    State.SuccessLoading(
                        people = People(
                            id = currentState.people.id,
                            name = currentState.people.name,

                            filmography = currentState.people.filmography + people.filmography
                        ),
                        hasMore = people.filmography.isNotEmpty(),
                    )
                )
            } catch (e: Exception) {
                Log.e("PeopleViewModel", "loadMorePeopleFilmography: ", e)
                _state.postValue(State.FailedLoading(e))
            }
        }
    }
}