package com.tanasi.streamflix.extractors

import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.models.Video
import com.tanasi.streamflix.utils.StringConverterFactory
import okhttp3.OkHttpClient
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url
import java.net.URI

open class DoodLaExtractor : Extractor() {

    override val name = "DoodStream"
    override val mainUrl = "https://dood.la"

    private val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

    override suspend fun extract(link: String): Video {
        val service = Service.build(mainUrl)

        val embedUrl = link.replace("/d/", "/e/")
        val document = service.get(embedUrl, link)

        val md5 = getBaseUrl(embedUrl) +
                (Regex("/pass_md5/[^']*").find(document.toString())?.value
                    ?: throw Exception("Can't find md5"))

        val url = service.getString(md5, link) +
                createHashTable() +
                "?token=${md5.substringAfterLast("/")}"

        val video = Video(
            source = url,
            headers = mapOf(
                "Referer" to mainUrl
            )
        )

        return video
    }

    private fun createHashTable(): String {
        return buildString {
            repeat(10) {
                append(alphabet.random())
            }
        }
    }

    private fun getBaseUrl(url: String) = URI(url).let { "${it.scheme}://${it.host}" }


    class DoodLiExtractor : DoodLaExtractor() {
        override var mainUrl = "https://dood.li"
    }

    class DoodExtractor : DoodLaExtractor() {
        override val mainUrl = "https://vide0.net"
    }


    private interface Service {

        companion object {
            fun build(baseUrl: String): Service {
                val client = OkHttpClient.Builder()
                    .build()

                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .addConverterFactory(StringConverterFactory.create())
                    .client(client)
                    .build()

                return retrofit.create(Service::class.java)
            }
        }


        @GET
        suspend fun get(
            @Url url: String,
            @Header("Referer") referer: String,
        ): Document

        @GET
        suspend fun getString(
            @Url url: String,
            @Header("Referer") referer: String,
        ): String
    }
}