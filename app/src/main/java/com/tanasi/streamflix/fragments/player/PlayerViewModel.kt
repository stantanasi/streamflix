package com.tanasi.streamflix.fragments.player

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.streamflix.models.Video
import com.tanasi.streamflix.utils.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(
    videoType: Video.Type,
    id: String,
) : ViewModel() {

    private val _state = MutableStateFlow<State>(State.LoadingServers)
    val state: Flow<State> = _state

    sealed class State {
        data object LoadingServers : State()
        data class SuccessLoadingServers(val servers: List<Video.Server>) : State()
        data class FailedLoadingServers(val error: Exception) : State()

        data class LoadingVideo(val server: Video.Server) : State()
        data class SuccessLoadingVideo(val video: Video, val server: Video.Server) : State()
        data class FailedLoadingVideo(val error: Exception, val server: Video.Server) : State()
    }

    init {
        getServers(videoType, id)
    }


    private fun getServers(
        videoType: Video.Type,
        id: String,
    ) = viewModelScope.launch(Dispatchers.IO) {
        _state.emit(State.LoadingServers)

        try {
            val servers = UserPreferences.currentProvider!!.getServers(id, videoType)

            if (servers.isEmpty()) throw Exception("No servers found")

            _state.emit(State.SuccessLoadingServers(servers))
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "getServers: ", e)
            _state.emit(State.FailedLoadingServers(e))
        }
    }

    fun getVideo(server: Video.Server) = viewModelScope.launch(Dispatchers.IO) {
        _state.emit(State.LoadingVideo(server))

        try {
            val video = UserPreferences.currentProvider!!.getVideo(server)

            if (video.source.isEmpty()) throw Exception("No source found")

            _state.emit(State.SuccessLoadingVideo(video, server))
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "getVideo: ", e)
            _state.emit(State.FailedLoadingVideo(e, server))
        }
    }
}