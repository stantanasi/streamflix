package com.tanasi.streamflix.extractors

import android.util.Base64
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.models.Video
import com.tanasi.streamflix.utils.DecryptHelper
import java.util.regex.Pattern
import okhttp3.OkHttpClient
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url

class MagaSavorExtractor : Extractor() {
    override val name = "MagaSavors"
    override val mainUrl = "https://magasavor.net"

    override suspend fun extract(link: String): Video {
        val service = Service.build(mainUrl)
        val source = service.get(link, mainUrl)
        val scriptTag = source.selectFirst("script[type=application/json]")
        val encodedStringInScriptTag = scriptTag?.data()?.trim().orEmpty()
        val encodedString = DecryptHelper.findEncodedRegex(source.html())
        val decryptedContent = if (encodedString != null) {
            DecryptHelper.decrypt(encodedString)
        } else {
            DecryptHelper.decrypt(encodedStringInScriptTag)
        }
        val m3u8 = decryptedContent.get("source")?.asString.orEmpty()

        return Video(
            source = m3u8,
            subtitles = listOf()
        )

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
