package com.tanasi.sflix.fragments.player

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.tanasi.sflix.models.Video
import com.tanasi.sflix.services.SflixService
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject

class PlayerViewModel : ViewModel() {

    private val sflixService = SflixService.build()

    private val _state = MutableLiveData<State>(State.Loading)
    val state: LiveData<State> = _state

    sealed class State {
        object Loading : State()

        data class SuccessLoading(val video: Video) : State()
        data class FailedLoading(val error: Exception) : State()
    }


    fun getVideo(videoType: PlayerFragment.VideoType, id: String) = viewModelScope.launch {
        _state.value = State.Loading

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

            OkHttpClient.Builder().build().newWebSocket(
                Request.Builder()
                    .url("wss://wsx.dokicloud.one/socket.io/?EIO=4&transport=websocket")
                    .build(),
                object : WebSocketListener() {
                    private lateinit var key: String

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        val response = Regex("^(\\d*)(.*)").find(text).let {
                            object {
                                val code = it?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
                                val data = it?.groupValues?.getOrNull(2) ?: ""
                            }
                        }

                        when (response.code) {
                            0 -> webSocket.send("40")
                            40 -> {
                                key = JSONObject(response.data).optString("sid")
                                val linkId =
                                    link.link.substringAfterLast("/").substringBefore("?")
                                webSocket.send("""42["getSources",{"id":"$linkId"}]""")
                            }
                            42 -> {
                                val json = JSONArray(response.data)

                                val sources = Gson().fromJson(
                                    json.opt(1).toString(),
                                    SflixService.Sources.Encrypted::class.java,
                                ).decrypt(
                                    secret = key
                                )

                                _state.postValue(State.SuccessLoading(
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
                                ))

                                webSocket.send("41")
                            }
                        }
                    }

                    override fun onFailure(
                        webSocket: WebSocket,
                        t: Throwable,
                        response: Response?
                    ) {
                        _state.postValue(State.FailedLoading(Exception(t)))
                    }
                }
            )
        } catch (e: Exception) {
            _state.value = State.FailedLoading(e)
        }
    }
}