package com.tanasi.streamflix.extractors

import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.models.Video
import okhttp3.OkHttpClient
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url


class VidPlyExtractor : Extractor() {
    override val name = "VidPly"
    override val mainUrl = "https://vidply.com/"

    override suspend fun extract(link: String): Video {
        val service = Service.build(mainUrl)

        val document = service.get(link.replace("/d/", "/e/", ignoreCase = true))

        val (passMd5Endpoint, token) = extractParameters(document)

        val baseVideoUrl = service.getVideoSource("$mainUrl$passMd5Endpoint", mainUrl)
            .trim()
            .takeIf { it.isNotEmpty() }
            ?: throw Exception("Empty video URL received")

        return Video(
            source = createSource(baseVideoUrl, token),
            headers = mapOf(
                "Referer" to mainUrl,
                "User-Agent" to USER_AGENT
            )
        )
    }

    private fun extractParameters(document: Document): Pair<String, String> {
        val docString = document.toString()

        // Extract the pass_md5 endpoint
        val passMd5Endpoint = PASS_MD5_REGEX.find(docString)?.groupValues?.get(1)
            ?: throw Exception("Could not find pass_md5 endpoint")

        // Extract the token from makePlay function
        val token = TOKEN_REGEX.find(docString)?.groupValues?.get(1)
            ?: throw Exception("Could not find token in makePlay function")

        return Pair(passMd5Endpoint, token)
    }

    private fun createSource(baseVideoUrl: String, token: String): String = buildString {
        if (baseVideoUrl.endsWith("~")) {
            append(baseVideoUrl)
            append(generateRandomString())
            append("?token=")
            append(token)
            append("&expiry=")
            append(System.currentTimeMillis())
        } else {
            append(baseVideoUrl)
        }
    }

    private fun generateRandomString(): String = buildString(10) {
        repeat(10) { append(ALLOWED_CHARS.random()) }
    }

    private interface Service {
        companion object {
            fun build(baseUrl: String): Service = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(OkHttpClient.Builder().build())
                .addConverterFactory(JsoupConverterFactory.create())
                .addConverterFactory(ScalarsConverterFactory.create())
                .build()
                .create(Service::class.java)
        }

        @GET
        suspend fun get(
            @Url url: String,
            @Header("Accept") accept: String = "text/html"
        ): Document

        @GET
        suspend fun getVideoSource(
            @Url url: String,
            @Header("Referer") referer: String
        ): String
    }

    companion object {
        private val PASS_MD5_REGEX = Regex("""\$\.get\('(/pass_md5/[^']*)'""")
        private val TOKEN_REGEX = Regex("""return\s*[^?]+\?token=([^&]+)&expiry=""")
        private const val ALLOWED_CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    }
}