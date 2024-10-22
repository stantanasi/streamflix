package com.tanasi.streamflix.extractors

import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.models.Video
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url

class MoviesapiExtractor : Extractor() {

    override val name = "Moviesapi"
    override val mainUrl = "https://moviesapi.club/"

    fun server(videoType: Video.Type): Video.Server {
        return Video.Server(
            id = name,
            name = name,
            src = when (videoType) {
                is Video.Type.Episode -> "$mainUrl/tv/${videoType.tvShow.id}-${videoType.season.number}-${videoType.number}"
                is Video.Type.Movie -> "$mainUrl/movie/${videoType.id}"
            },
        )
    }

    override suspend fun extract(link: String): Video {
        val service = Service.build(mainUrl)

        val iframe = service.get(link, referer = "https://pressplay.top/")
            .selectFirst("iframe")
            ?.attr("src")
            ?: throw Exception("Can't retrieve iframe")

        return ChillxExtractor.MoviesapiExtractor().extract(iframe)
    }

    private interface Service {

        companion object {
            fun build(baseUrl: String): Service {
                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .build()

                return retrofit.create(Service::class.java)
            }
        }

        @GET
        suspend fun get(
            @Url url: String,
            @Header("referer") referer: String = "",
        ): Document
    }
}