package com.tanasi.streamflix.extractors

import androidx.media3.common.MimeTypes
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.models.Video
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Url

class VixcloudExtractor : Extractor() {

    override val name = "vixcloud"
    override val mainUrl = "https://vixcloud.co/"

    override suspend fun extract(link: String): Video {
        val service = VixcloudExtractorService.build(mainUrl)
        val source = service.getSource(link.replace(mainUrl, ""))

        // parsing json info
        val scriptText = source.body().selectFirst("script")?.data() ?: ""

        val videoJson = scriptText
            .substringAfter("window.video = ")
            .substringBefore(";")
        val masterPlaylistJson = scriptText
            .substringAfter("window.masterPlaylist")
            .substringAfter("params: ")
            .replace("'token'", "token")
            .replace("'expires'", "expires")
            .replace("'", "\"")
            .substringBefore("}")
            .substringBeforeLast(",") + "}"

        val hasBParam = scriptText
            .substringAfter("url:")
            .substringBefore(",")
            .contains("b=1")

        val gson = Gson()
        val windowVideo = gson.fromJson(videoJson, VixcloudExtractorService.WindowVideo::class.java)
        val masterPlaylist = gson.fromJson(masterPlaylistJson, VixcloudExtractorService.WindowParams::class.java)

        //
        val masterParams = mutableMapOf(
            "token" to masterPlaylist.token,
            "expires" to masterPlaylist.expires
        )

        // parse parameters from source url
        val currentParams = link
            .split("&")
            .map { param -> param.split("=") }
            .filter { it.size == 2 }
            .associate { it[0] to it[1] }

        if (hasBParam)
            masterParams["b"] = "1"

        if (currentParams.containsKey("canPlayFHD"))
            masterParams["h"] = "1"

        // final playlist url
        val baseUrl = "https://vixcloud.co/playlist/${windowVideo.id}"

        val httpUrlBuilder = baseUrl.toHttpUrlOrNull()?.newBuilder()
            ?: throw IllegalArgumentException("Invalid base URL")

        for ((key, value) in masterParams)
            httpUrlBuilder.addQueryParameter(key, value)

        val finalUrl = httpUrlBuilder.build().toString()

        return Video(
            source = finalUrl,
            subtitles = listOf(),
            type = MimeTypes.APPLICATION_M3U8,
            headers = mapOf(
                "Referer" to mainUrl,
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
            )
        )
    }

    private interface VixcloudExtractorService {

        companion object {
            fun build(baseUrl: String): VixcloudExtractorService {
                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .build()

                return retrofit.create(VixcloudExtractorService::class.java)
            }
        }

        @GET
        @Headers("User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
        suspend fun getSource(@Url url: String): Document

        data class WindowVideo(
            @SerializedName("id") val id: Int,
            @SerializedName("filename") val filename: String
        )

        data class WindowParams(
            @SerializedName("token") val token: String,
            @SerializedName("expires") val expires: String
        )
    }
}
