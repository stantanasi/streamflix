package com.tanasi.streamflix.extractors

import android.util.Base64
import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.models.Video
import com.tanasi.streamflix.utils.JsUnpacker
import com.tanasi.streamflix.utils.retry
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url
import java.net.URI

class MoflixExtractor : Extractor() {

    override val name = "Moflix"
    override val mainUrl = "https://moflix-stream.xyz"

    suspend fun server(videoType: Video.Type): Video.Server {
        val service = Service.build(mainUrl)

        return Video.Server(
            id = name,
            name = name,
            src = when (videoType) {
                is Video.Type.Episode -> {
                    val id = Base64.encode("tmdb|series|${videoType.tvShow.id}".toByteArray(), Base64.NO_WRAP).toString(Charsets.UTF_8)
                    val mediaId = try {
                        service.getResponse(
                            "$mainUrl/api/v1/titles/$id?loader=titlePage",
                            referer = mainUrl
                        ).title?.id
                    } catch (_: Exception) {
                        id
                    }
                    "$mainUrl/api/v1/titles/$mediaId/seasons/${videoType.season.number}/episodes/${videoType.number}?loader=episodePage"
                }
                is Video.Type.Movie -> {
                    val id = Base64.encode("tmdb|movie|${videoType.id}".toByteArray(), Base64.NO_WRAP).toString(Charsets.UTF_8)
                    "$mainUrl/api/v1/titles/$id?loader=titlePage"
                }
            },
        )
    }

    override suspend fun extract(link: String): Video {
        val service = Service.build(mainUrl)
        
        val res = service.getResponse(link, referer = mainUrl)
        
        val frames = (res.episode ?: res.title)?.videos?.filter { it.category.equals("full", true) }
            ?: throw Exception("No frames found")
        
        val video = retry(frames.size) { attempt ->
            val iframe = frames[attempt - 1]

            val response = service.get(
                iframe.src ?: throw Exception("src is null"),
                referer = mainUrl
            )
            val host = URI(iframe.src).let { "${it.scheme}://${it.host}" }
            val doc = response.selectFirst("script:containsData(sources:)")?.data()
            val script = if (doc.isNullOrEmpty()) {
                val packedRegex = Regex("""eval\(function\(p,a,c,k,e,.*\)\)""")
                val packedText = packedRegex.find(response.text())?.value
                JsUnpacker(packedText).unpack() ?: response.text()
            } else {
                doc
            }

            val m3u8 = Regex("file:\\s*\"(.*?m3u8.*?)\"").find(
                script ?: throw Exception("Script is null")
            )?.groupValues?.getOrNull(1)
                ?: throw Exception("Can't find m3u8")

            suspend fun String.haveDub(referer: String) : Boolean {
                return service.get(this, referer=referer).text().contains("TYPE=AUDIO")
            }

            if (!m3u8.haveDub("$host/"))
                throw Exception("Video don't have dub")

            Video(
                m3u8,
                headers = mapOf(
                    "Referer" to "$host/",
                ),
            )
        }

        return video
    }


    private interface Service {

        companion object {
            fun build(baseUrl: String): Service {
                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                return retrofit.create(Service::class.java)
            }
        }

        @GET
        suspend fun get(
            @Url url: String,
            @Header("referer") referer: String,
        ): Document

        @GET
        suspend fun getResponse(
            @Url url: String,
            @Header("referer") referer: String,
        ): MoflixResponse
    }


    data class MoflixResponse(
        val title: Episode? = null,
        val episode: Episode? = null,
    ) {
        data class Episode(
            val id: Int? = null,
            val videos: List<Videos>? = listOf(),
        ) {
            data class Videos(
                val name: String? = null,
                val category: String? = null,
                val src: String? = null,
                val quality: String? = null,
            )
        }
    }
}