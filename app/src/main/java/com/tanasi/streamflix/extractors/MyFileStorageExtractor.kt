package com.tanasi.streamflix.extractors

import com.tanasi.streamflix.models.Video
import com.tanasi.streamflix.utils.StringConverterFactory
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Streaming
import retrofit2.http.Url

class MyFileStorageExtractor : Extractor() {

    override val name = "MyFileStorage"
    override val mainUrl = "https://myfilestorage.xyz"

    fun nowTvServer(videoType: Video.Type): Video.Server {
        return Video.Server(
            id = name,
            name = name,
            src = when (videoType) {
                is Video.Type.Episode -> {
                    val episode = when {
                        videoType.number < 10 -> "0${videoType.number}"
                        else -> "${videoType.number}"
                    }
                    "$mainUrl/tv/${videoType.id}/s${videoType.season.number}e${episode}.mp4"
                }

                is Video.Type.Movie -> "$mainUrl/${videoType.id}.mp4"
            },
        )
    }

    override suspend fun extract(link: String): Video {
        val service = Service.build(mainUrl)

        suspend fun String.isSuccess(): Boolean {
            return try {
                service.get(this, referer = "https://bflix.gs/").isSuccessful
            } catch (e: Exception) {
                false
            }
        }

        var url = link

        if (!url.isSuccess()) {
            url = link.replace(".mp4", "-1.mp4")

            if (!url.isSuccess()) {
                url = link
                if (!url.isSuccess()) throw Exception("404 not found")
            }
        }

        val video = Video(
            source = url,
            subtitles = emptyList(),
            headers = mapOf(
                "Referer" to "https://bflix.gs/",
            ),
        )

        return video
    }


    private interface Service {

        companion object {
            fun build(baseUrl: String): Service {
                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(StringConverterFactory.create())
                    .build()

                return retrofit.create(Service::class.java)
            }
        }

        @GET
        @Streaming
        suspend fun get(
            @Url url: String,
            @Header("referer") referer: String = "",
        ): Response<ResponseBody>
    }
}