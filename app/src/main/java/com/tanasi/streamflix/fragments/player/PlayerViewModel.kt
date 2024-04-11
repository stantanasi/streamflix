package com.tanasi.streamflix.fragments.player

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.streamflix.models.Video
import com.tanasi.streamflix.utils.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PlayerViewModel(
    videoType: Video.Type,
    id: String,
) : ViewModel() {

    private val _state = MutableLiveData<State>(State.LoadingServers)
    val state: LiveData<State> = _state

    sealed class State {
        data object LoadingServers : State()
        data class SuccessLoadingServers(val servers: List<Video.Server>) : State()
        data class FailedLoadingServers(val error: Exception) : State()

        data object LoadingVideo : State()
        data class SuccessLoadingVideo(val video: Video, val server: Video.Server) : State()
        data class FailedLoadingVideo(val error: Exception) : State()
    }

    init {
        getServers(videoType, id)
    }


    private fun getServers(
        videoType: Video.Type,
        id: String,
    ) = viewModelScope.launch(Dispatchers.IO) {
        _state.postValue(State.LoadingServers)

        try {
            val servers = UserPreferences.currentProvider!!.getServers(id, videoType)

            if (servers.isEmpty()) throw Exception("No servers found")

            _state.postValue(State.SuccessLoadingServers(servers))
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "getServers: ", e)
            _state.postValue(State.FailedLoadingServers(e))
        }
    }

    fun getVideo(server: Video.Server) = viewModelScope.launch(Dispatchers.IO) {
        _state.postValue(State.LoadingVideo)

        try {
            val video = UserPreferences.currentProvider!!.getVideo(server)

            if (video.source.isEmpty()) throw Exception("No source found")

            _state.postValue(State.SuccessLoadingVideo(video, server))
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "getVideo: ", e)
            _state.postValue(State.FailedLoadingVideo(e))
        }
    }
}