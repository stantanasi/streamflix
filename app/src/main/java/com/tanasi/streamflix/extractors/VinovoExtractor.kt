//package com.tanasi.streamflix.extractors
//
//import android.util.Base64
//import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
//import com.tanasi.streamflix.models.Video
//import com.tanasi.streamflix.utils.UserPreferences
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import okhttp3.FormBody
//import okhttp3.OkHttpClient
//import okhttp3.Request
//import org.jsoup.nodes.Document
//import retrofit2.Retrofit
//import retrofit2.converter.gson.GsonConverterFactory
//import retrofit2.converter.scalars.ScalarsConverterFactory
//import retrofit2.http.*
//import java.net.URLEncoder
//import java.util.regex.Pattern
//
//open class VinovoExtractor : Extractor() {
//
//    override val name = "Vinovo"
//    override val mainUrl = "https://vinovo.to"
//    override val aliasUrls = listOf("vinovo.to", "vinovo.si")
//
//    private val pattern = Regex("""(?://|\.)(vinovo\.(?:to|si))/(?:e|d)/([0-9a-zA-Z]+)""")
//
//    companion object {
//        const val DEFAULT_USER_AGENT =
//            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/115.0"
//    }
//
//    override suspend fun extract(link: String): Video {
//        val match = pattern.find(link) ?: throw Exception("Invalid Vinovo URL")
//        val (host, mediaId) = match.destructured
//
//        val rurl = "https://$host/"
//        val webUrl = "${rurl}e/$mediaId"
//
//        val service = Service.build(mainUrl)
//
//        // 1. Get page HTML
//        val htmlDoc = service.getSource(
//            webUrl,
//            referer = webUrl,
//            origin = rurl.removeSuffix("/"),
//            userAgent = DEFAULT_USER_AGENT
//        )
//        val html = htmlDoc.toString()
//
//        // 2. Extract token & dataBase from HTML
//        val token = Regex("""name="token"\s*content="([^"]+)""").find(html)?.groupValues?.get(1)
//            ?: throw Exception("Token not found")
//        val dataBase = Regex("""<video.+?data-base="([^"]+)""").find(html)?.groupValues?.get(1)
//            ?: throw Exception("Data base not found")
//
//        val resp = service.postPlay(
//            apiUrl = "https://vinovo.to/api/file/play/$mediaId",
//            ab = "0",
//            referrer = UserPreferences.currentProvider!!.baseUrl,
//            token = token,
//            headers = mapOf(
//                "User-Agent" to DEFAULT_USER_AGENT,
//                "Referer" to mainUrl,
//                "Origin" to link,
//                "X-Requested-With" to "XMLHttpRequest"
//            )
//        )
//
//        if (resp.status == "ok") {
//            val finalToken = resp.token ?: throw Exception("Missing video token")
//            val videoUrl = "$dataBase/stream/$finalToken"
//            return Video(
//                source = videoUrl,
//                headers = mapOf(
//                    "User-Agent" to DEFAULT_USER_AGENT,
//                    "Referer" to UserPreferences.currentProvider!!.baseUrl
//                )
//            )
//        }
//
//        throw Exception("File Not Found or Removed")
//    }
//
//    /**
//     * This function mimics your Python girc to get recaptcha token.
//     * It performs necessary requests to google recaptcha endpoints.
//     */
//    private suspend fun getRecaptchaToken(pageData: String, url: String, service: Service): String = withContext(Dispatchers.IO) {
//        val rurlBase = "https://www.google.com/recaptcha/api.js"
//        val aurlBase = "https://www.google.com/recaptcha/api2"
//
//        val userAgent = DEFAULT_USER_AGENT
//
//        // Extract site key from pageData
//        val siteKeyRegex = Regex("""(?:src="$rurlBase\?.*?render|data-sitekey)="?([^"]+)""")
//        val keyMatch = siteKeyRegex.find(pageData) ?: return@withContext ""
//        val siteKey = keyMatch.groupValues[1]
//
//        // Compute co param
//        val coRaw = url.dropLast(1) + ":443"
//        val coEncoded = Base64.encodeToString(coRaw.toByteArray(), Base64.NO_PADDING or Base64.NO_WRAP)
//        val co = coEncoded.replace("=", "")
//
//        val headers = mapOf(
//            "User-Agent" to userAgent,
//            "Referer" to mainUrl
//        )
//
//        // 1) GET recaptcha api with render key
//        val rurl = "$rurlBase?render=$siteKey"
//        val pageData1 = service.rawGet(rurl, headers)
//        val vMatch = Regex("""releases/([^/]+)""").find(pageData1) ?: return@withContext ""
//        val v = vMatch.groupValues[1]
//        val cb = (1..16)
//            .map { ('a'..'z') + ('0'..'9') }
//            .flatten()
//            .shuffled()
//            .take(12)
//            .joinToString("")
//        // 2) GET anchor endpoint
//        val rdata = mapOf(
//            "ar" to "1",
//            "k" to siteKey,
//            "co" to co,
//            "hl" to "en",
//            "v" to v,
//            "size" to "invisible",
//            "cb" to cb
//        )
//        val anchorUrl = "$aurlBase/anchor?" + rdata.toQueryString()
//        val recaptchaHeaders = mapOf(
//            "Host" to "www.google.com",
//            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:139.0) Gecko/20100101 Firefox/139.0",
//            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
//            "Accept-Language" to "de,en-US;q=0.7,en;q=0.3",
//            "DNT" to "1",
//            "Sec-GPC" to "1",
//            "Referer" to "$mainUrl/",
//            "Upgrade-Insecure-Requests" to "1",
//            "Sec-Fetch-Dest" to "iframe",
//            "Sec-Fetch-Mode" to "navigate",
//            "Sec-Fetch-Site" to "cross-site",
//            "Connection" to "keep-alive",
//            "TE" to "trailers",
//            "Alt-Used" to "www.google.com",
//            "Priority" to "u=4"
//            // Note: TE and Priority headers may not be needed unless your HTTP client supports them
//        )
//        val pageData2 = service.rawGet(anchorUrl, recaptchaHeaders)
//        val rtokenMatch = Regex("""recaptcha-token.+?="([^"]+)""").find(pageData2) ?: return@withContext ""
//        val rtoken = rtokenMatch.groupValues[1]
//
//        // 3) POST reload endpoint
//        val pdata = mapOf(
//            "v" to v,
//            "reason" to "q",
//            "k" to siteKey,
//            "c" to rtoken,
//            "sa" to "",
//            "co" to co
//        )
//        val postHeaders = headers + mapOf("Referer" to aurlBase)
//        val pageData3 = service.rawPost("$aurlBase/reload?k=$siteKey", pdata, postHeaders)
//        val gtokenMatch = Regex("""rresp","([^"]+)""").find(pageData3) ?: return@withContext ""
//
//        return@withContext gtokenMatch.groupValues[1]
//    }
//
//
//    // Helper: Http GET with OkHttp
//
//    private fun Map<String, String>.toQueryString(): String =
//        this.entries.joinToString("&") { (k, v) -> "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}" }
//
//    private interface Service {
//        companion object {
//            fun build(baseUrl: String): Service {
//                val retrofit = Retrofit.Builder()
//                    .baseUrl(baseUrl)
//                    .addConverterFactory(ScalarsConverterFactory.create())  // This goes first — tries plain text
//                    .addConverterFactory(JsoupConverterFactory.create())    // Then tries Jsoup
//                    .addConverterFactory(GsonConverterFactory.create())     // Finally tries Gson for JSON
//                    .build()
//                return retrofit.create(Service::class.java)
//            }
//
//        }
//
//        @GET
//        suspend fun getSource(
//            @Url url: String,
//            @Header("Referer") referer: String,
//            @Header("Origin") origin: String,
//            @Header("User-Agent") userAgent: String
//        ): Document
//
//        @FormUrlEncoded
//        @POST
//        suspend fun postPlay(
//            @Url apiUrl: String,
//            @Field("ab") ab: String,
//            @Field("referrer") referrer: String,
//            @Field("token") token: String,
//            @HeaderMap headers: Map<String, String>
//        ): VinovoResponse
//
//        @FormUrlEncoded
//        @POST
//        suspend fun postRecaptcha(
//            @Url apiUrl: String,
//            @Field("recaptcha") recaptcha: String,
//            @Field("token") token: String,
//            @HeaderMap headers: Map<String, String>
//        ): VinovoResponse
//        @GET
//        suspend fun rawGet(
//            @Url url: String,
//            @HeaderMap headers: Map<String, String>
//        ): String
//
//        // Add raw POST with form data + custom headers
//        @FormUrlEncoded
//        @POST
//        suspend fun rawPost(
//            @Url url: String,
//            @FieldMap formData: Map<String, String>,
//            @HeaderMap headers: Map<String, String>
//        ): String
//    }
//
//    private data class VinovoResponse(
//        val status: String,
//        val token: String?
//    )
//}
