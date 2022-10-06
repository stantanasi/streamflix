package com.tanasi.sflix.fragments.player

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.sflix.models.Video
import com.tanasi.sflix.services.SflixService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PlayerViewModel : ViewModel() {

    private val sflixService = SflixService.build()

    private val _state = MutableLiveData<State>(State.Loading)
    val state: LiveData<State> = _state

    sealed class State {
        object Loading : State()

        data class SuccessLoading(val video: Video) : State()
        data class FailedLoading(val error: Exception) : State()
    }


    fun getVideo(
        videoType: PlayerFragment.VideoType,
        id: String
    ) = viewModelScope.launch(Dispatchers.IO) {
        _state.postValue(State.Loading)

        try {
            val servers = when (videoType) {
                PlayerFragment.VideoType.Movie -> sflixService.getMovieServersById(id)
                PlayerFragment.VideoType.Episode -> sflixService.getEpisodeServersById(id)
            }.select("a").map {
                object {
                    val id = it.attr("data-id")
                    val name = it.selectFirst("span")?.text()?.trim() ?: ""
                }
            }

            val link = sflixService.getLink(servers.firstOrNull()?.id ?: "")

            val response = sflixService.getSources(
                url = link.link
                    .substringBeforeLast("/")
                    .replace("/embed", "/ajax/embed")
                    .plus("/getSources"),
                id = link.link.substringAfterLast("/").substringBefore("?"),
            )

            val sources = when (response) {
                is SflixService.Sources -> response
                is SflixService.Sources.Encrypted -> response.decrypt(
                    secret = sflixService.getSourceEncryptedKey().text()
                )
            }

            val video = Video(
                source = sources.sources.firstOrNull()?.file ?: "",
                subtitles = sources.tracks
                    .filter { it.kind == "captions" }
                    .map {
                        Video.Subtitle(
                            label = it.label,
                            file = it.file,
                            default = it.default,
                        )
                    }
            )

            _state.postValue(State.SuccessLoading(video))
        } catch (e: Exception) {
            _state.postValue(State.FailedLoading(e))
        }
    }
}