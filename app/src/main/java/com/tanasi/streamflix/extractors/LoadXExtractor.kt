package com.tanasi.streamflix.extractors

import androidx.media3.common.MimeTypes
import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.models.Video
import com.tanasi.streamflix.utils.JsUnpacker
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import org.json.JSONObject
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

open class LoadXExtractor: Extractor() {
    override val name = "LoadX"
    override val mainUrl = "https://loadx.ws/"

    override suspend fun extract(link: String): Video {
        val videoId = link.substringAfterLast("/")

        val client = OkHttpClient()

        val getRequest = Request.Builder()
            .url(link)
            .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:139.0) Gecko/20100101 Firefox/139.0")
            .build()

        val getResponse = client.newCall(getRequest).execute()
        val setCookieHeaders = getResponse.headers("Set-Cookie")

        val firePlayerCookie = setCookieHeaders
            .firstOrNull { it.startsWith("fireplayer_player=") }
            ?.split(";")?.get(0)
            ?: throw Exception("fireplayer_player cookie not found")

        getResponse.close()
        val service = Service.build(mainUrl)

        val responseBody = service.postVideoData(
            data = videoId,
            cookie = firePlayerCookie
        )
        val videoUrl = JSONObject(responseBody.string()).optString("videoSource")
            ?: throw Exception("videoSource not found in response")

        return Video(source = videoUrl,
            type = MimeTypes.APPLICATION_M3U8,
            headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:139.0) Gecko/20100101 Firefox/139.0",
                "Referer" to mainUrl,
                "Origin" to mainUrl,
                "Accept" to "*/*",
                "Accept-Language" to "en-US,en;q=0.5",
                "Connection" to "keep-alive",
                "Cookie" to firePlayerCookie)
        )
    }



    private interface Service {
        @GET
        suspend fun getSource(@Url url: String): Document
        @GET
        suspend fun getRequest(@Url url: String): Response

        @GET
        suspend fun getWithHeaders(
            @Url url: String,
            @Header("Referer") referer: String,
            @Header("User-Agent") userAgent: String = DEFAULT_USER_AGENT
        ): Document

        @POST("player/index.php")
        @Headers(
            "Accept: */*",
            "Accept-Language: de,en-US;q=0.7,en;q=0.3",
            "Content-Type: application/x-www-form-urlencoded; charset=UTF-8",
            "X-Requested-With: XMLHttpRequest",
            "Origin: https://loadx.ws",
            "Sec-GPC: 1",
            "Connection: keep-alive",
            "Sec-Fetch-Dest: empty",
            "Sec-Fetch-Mode: cors",
            "Sec-Fetch-Site: same-origin"
        )
        suspend fun postVideoData(
            @Query("data") data: String,
            @Query("do") doValue: String = "getVideo",
            @Header("User-Agent") userAgent: String = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:139.0) Gecko/20100101 Firefox/139.0",
            @Header("Referer") referer: String = "https://loadx.ws",
            @Header("Cookie") cookie: String,
            @Body body: RequestBody = RequestBody.create(null, "") // empty body
        ): ResponseBody

        companion object {
            private const val DEFAULT_USER_AGENT =
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/115.0"

            fun build(baseUrl: String): Service {
                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .build()

                return retrofit.create(Service::class.java)
            }
        }
    }

}