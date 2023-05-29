package com.tanasi.sflix.fragments.player

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.sflix.models.Video
import com.tanasi.sflix.utils.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PlayerViewModel(
    videoType: PlayerFragment.VideoType,
    id: String,
) : ViewModel() {

    private val _state = MutableLiveData<State>(State.Loading)
    val state: LiveData<State> = _state

    sealed class State {
        object Loading : State()
        data class SuccessLoading(val video: Video) : State()
        data class FailedLoading(val error: Exception) : State()
    }

    init {
        getVideo(videoType, id)
    }


    fun getVideo(
        videoType: PlayerFragment.VideoType,
        id: String,
    ) = viewModelScope.launch(Dispatchers.IO) {
        _state.postValue(State.Loading)

        try {
            val video = UserPreferences.currentProvider.getVideo(id, videoType)

            if (video.sources.isEmpty()) throw Exception("No links found")

            _state.postValue(State.SuccessLoading(video))
        } catch (e: Exception) {
            _state.postValue(State.FailedLoading(e))
        }
    }
}