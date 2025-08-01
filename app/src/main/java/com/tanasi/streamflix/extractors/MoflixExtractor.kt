package com.tanasi.streamflix.extractors

import android.util.Base64
import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.models.Video
import com.tanasi.streamflix.utils.JsUnpacker
import com.tanasi.streamflix.utils.retry
import okhttp3.ResponseBody
import org.jsoup.nodes.Document
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url
import java.net.URI
import java.net.URL

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
    val embedResponse = service.getRawResponse(link, referer = mainUrl)
    val cookies = embedResponse.headers()
        .values("Set-Cookie")
        .joinToString("; ") { it.substringBefore(";") }

    val embedHtml = embedResponse.body()?.string() ?: throw Exception("Empty embed page body")

    val packedJS = Regex("(eval\\(function\\(p,a,c,k,e,d\\)(.|\\n)*?)</script>")
        .find(embedHtml)
        ?.groupValues?.get(1)
        ?: throw Exception("Packed JS not found")
    val script = JsUnpacker(packedJS).unpack() ?: embedHtml

    val m3u8 = Regex("file:\\s*\"(.*?m3u8.*?)\"")
        .find(script)
        ?.groupValues?.getOrNull(1)
        ?: throw Exception("Can't find m3u8")
    return Video(
        m3u8,
        headers = mapOf(
            "Referer" to link,
            "Cookie" to cookies,
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0",
            "X-Requested-With" to "XMLHttpRequest",
            "Accept" to "*/*",
            "Sec-Fetch-Site" to "same-origin",
            "Sec-Fetch-Mode" to "cors",
            "Sec-Fetch-Dest" to "empty"
        )
    )
}



    private interface Service {

        companion object {
            private const val DEFAULT_USER_AGENT =
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/115.0"
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
        @GET
        suspend fun getHTMLResponse(
            @Url url: String,
            @Header("Referer") referer: String,
            @Header("User-Agent") userAgent: String = DEFAULT_USER_AGENT,
            @Header("Accept") accept: String = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        @Header("Accept-Language") acceptLanguage: String = "en-US,en;q=0.5",
        @Header("Connection") connection: String = "keep-alive"
        ): Document
        @GET
        suspend fun getRawResponse(
            @Url url: String,
            @Header("Referer") referer: String,
            @Header("User-Agent") userAgent: String = DEFAULT_USER_AGENT,
            @Header("Accept") accept: String = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            @Header("Accept-Language") acceptLanguage: String = "en-US,en;q=0.5",
            @Header("Connection") connection: String = "keep-alive"
        ): Response<ResponseBody>
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