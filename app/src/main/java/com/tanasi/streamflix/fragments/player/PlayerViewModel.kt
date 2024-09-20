package com.tanasi.streamflix.fragments.player

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.streamflix.models.Video
import com.tanasi.streamflix.utils.OpenSubtitles
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

        data object LoadingSubtitles : State()
        data class SuccessLoadingSubtitles(val subtitles: List<OpenSubtitles.Subtitle>) : State()
        data class FailedLoadingSubtitles(val error: Exception) : State()

        data object DownloadingOpenSubtitle : State()
        data class SuccessDownloadingOpenSubtitle(
            val subtitle: OpenSubtitles.Subtitle,
            val uri: Uri
        ) : State()
        data class FailedDownloadingOpenSubtitle(
            val error: Exception,
            val subtitle: OpenSubtitles.Subtitle
        ) : State()
    }

    init {
        getServers(videoType, id)
        getSubtitles(videoType)
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

            video.subtitles
                .firstOrNull { it.label.startsWith(UserPreferences.subtitleName ?: "") }
                ?.default = true

            _state.emit(State.SuccessLoadingVideo(video, server))
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "getVideo: ", e)
            _state.emit(State.FailedLoadingVideo(e, server))
        }
    }

    private fun getSubtitles(videoType: Video.Type) = viewModelScope.launch(Dispatchers.IO) {
        _state.emit(State.LoadingSubtitles)

        try {
            val subtitles = when (videoType) {
                is Video.Type.Episode -> OpenSubtitles.search(
                    query = videoType.tvShow.title,
                    season = videoType.season.number,
                    episode = videoType.number,
                )

                is Video.Type.Movie -> OpenSubtitles.search(
                    query = videoType.title
                )
            }.sortedWith(compareBy({ it.languageName }, { it.subDownloadsCnt }))

            _state.emit(State.SuccessLoadingSubtitles(subtitles))
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "getSubtitles: ", e)
            _state.emit(State.FailedLoadingSubtitles(e))
        }
    }

    fun downloadSubtitle(subtitle: OpenSubtitles.Subtitle) = viewModelScope.launch(Dispatchers.IO) {
        _state.emit(State.DownloadingOpenSubtitle)

        try {
            val uri = OpenSubtitles.download(subtitle)

            _state.emit(State.SuccessDownloadingOpenSubtitle(subtitle, uri))
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "downloadSubtitle: ", e)
            _state.emit(State.FailedDownloadingOpenSubtitle(e, subtitle))
        }
    }
}