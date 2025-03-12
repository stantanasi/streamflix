package com.tanasi.streamflix.extractors

import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.models.Video
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url
import org.jsoup.nodes.Document
import okhttp3.OkHttpClient
import java.util.regex.Pattern

open class VidMoLyExtractor : Extractor() {
    override val name = "VidMoLy"
    override val mainUrl = "https://vidmoly.me/"
    private val redirectUrl = "https://vidmoly.to/"

    override suspend fun extract(link: String): Video {
        val service = Service.build(redirectUrl)

        val document = service.get(link.replace(".me/", ".to/"), redirectUrl)

        val hlsUrl = extractHlsUrl(document)
            ?: throw Exception("Could not find HLS source in the webpage")

        return Video(
            source = hlsUrl,
            headers = mapOf(
                "Referer" to redirectUrl,
                "User-Agent" to USER_AGENT
            )
        )
    }

    private fun extractHlsUrl(document: Document): String? {
        val pattern = Pattern.compile("sources:\\s*\\[\\{file:\\s*\"([^\"]+)\"\\}]")
        val matcher = pattern.matcher(document.toString())
        return if (matcher.find()) {
            matcher.group(1)
        } else {
            null
        }
    }

    class ToDomain: VidMoLyExtractor(){
        override val mainUrl: String = "https://vidmoly.to/"
    }

    private interface Service {
        companion object {
            fun build(baseUrl: String): Service = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(OkHttpClient.Builder().build())
                .addConverterFactory(JsoupConverterFactory.create())
                .build()
                .create(Service::class.java)
        }

        @GET
        suspend fun get(
            @Url url: String,
            @Header("Referer") referer: String,
            @Header("Accept") accept: String = "text/html",
            @Header("User-Agent") userAgent: String = USER_AGENT,
        ): Document
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
    }
}