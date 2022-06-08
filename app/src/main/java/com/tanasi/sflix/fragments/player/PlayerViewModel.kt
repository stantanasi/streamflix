package com.tanasi.sflix.fragments.player

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.sflix.models.Video
import com.tanasi.sflix.services.SflixService
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

    fun getVideo(id: String) = viewModelScope.launch {
        _state.value = State.Loading

        _state.value = try {
            val link = sflixService.getLink(id)

            val sources = sflixService.getSources(
                url = link.link
                    .substringBeforeLast("/")
                    .replace("/embed", "/ajax/embed")
                    .plus("/getSources"),
                id = link.link.substringAfterLast("/").substringBefore("?"),
            )

            State.SuccessLoading(
                Video(
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
            )
        } catch (e: Exception) {
            State.FailedLoading(e)
        }
    }
}