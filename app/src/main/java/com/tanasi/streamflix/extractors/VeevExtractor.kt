package com.tanasi.streamflix.extractors


import androidx.media3.common.MimeTypes
import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.models.Video
import com.tanasi.streamflix.utils.UserPreferences
import okhttp3.ResponseBody
import org.json.JSONObject

import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url
import java.net.URL
import java.net.URLEncoder
import kotlin.text.Charsets.UTF_8

open class VeevExtractor : Extractor() {
    override val name = "Veev"
    override val mainUrl = "https://veev.to"

    override val aliasUrls= listOf("https://veev.to", "https://kinoger.pw", "https://poophq.com", "https://doods.to")
    private val pattern = Regex("""(?://|\.)((?:veev|kinoger|poophq|doods)\.(?:to|pw|com))/(?:e|d)/([0-9a-zA-Z]+)""")

    companion object {
        const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/115.0"
    }

    override suspend fun extract(link: String): Video {
        val match = pattern.find(link) ?: throw Exception("Invalid Veev URL")
        val (host, mediaId) = match.destructured

        val webUrl = "https://$host/e/$mediaId"
        val service = Service.build("https://$host")

        val headers = mapOf(
            "User-Agent" to DEFAULT_USER_AGENT,
            "Referer" to UserPreferences.currentProvider!!.baseUrl,
            "Origin" to UserPreferences.currentProvider!!.baseUrl
        )

        val response = service.getWithHeaders(webUrl, headers["Referer"]!!, headers["User-Agent"]!!, webUrl)
        val itemsRegex = Regex("""[\.\s'](?:fc|_vvto\[[^\]]*)(?:['\]]*)?\s*[:=]\s*['"]([^'"]+)""")
        val items = itemsRegex.findAll(response.string()).map { it.groupValues[1] }.toList()

        if (items.isNotEmpty()) {
            for (f in items.asReversed()) {
                val ch = veevDecode(f)
                if (ch != f) {
                    val params = mapOf(
                        "op" to "player_api",
                        "cmd" to "gi",
                        "file_code" to mediaId,
                        "r" to URLEncoder.encode(UserPreferences.currentProvider!!.baseUrl),
                        "ch" to ch,
                        "ie" to "1"
                    )
                    val mainLink = URL(link).protocol + "://" + URL(link).host
                    val downloadUrl = "$mainLink/dl?" + params.map { "${it.key}=${it.value}" }.joinToString("&")
                    val jsonResponse = service.getWithHeaders(downloadUrl, UserPreferences.currentProvider!!.baseUrl, headers["User-Agent"]!!, mainUrl).string()

                    val fileJson = JSONObject(jsonResponse).optJSONObject("file")
                        ?: throw Exception("Video removed")

                    if (fileJson.optString("file_status") == "OK") {
                        val dv = fileJson.getJSONArray("dv").getJSONObject(0).getString("s")
                        val sourceUrl = decodeUrl(veevDecode(dv), buildArray(ch)[0])
                        val fileMimeType = fileJson.optString("file_mime_type", "")
                        val exoMimeType = fileMimeType.toExoPlayerMimeType()
                        return Video(source = sourceUrl, type = exoMimeType, headers = headers)
                    } else {
                        throw Exception("Video removed")
                    }
                }
            }
            throw Exception("Unable to locate video")
        } else {
            throw Exception("Video removed")
        }
    }
    fun String.toExoPlayerMimeType(): String {
        return when (this.lowercase()) {
            "video/x-matroska", "video/webm" -> MimeTypes.VIDEO_MATROSKA
            "video/mp4" -> MimeTypes.VIDEO_MP4
            "application/x-mpegurl", "application/vnd.apple.mpegurl" -> MimeTypes.APPLICATION_M3U8
            "video/avi" -> MimeTypes.VIDEO_AVI
            else -> ""
        }
    }


    private fun veevDecode(etext: String): String {
        val result = StringBuilder()
        val lut = mutableMapOf<Int, String>()
        var n = 256
        var c = etext[0].toString()
        result.append(c)

        for (char in etext.drop(1)) {
            val code = char.code
            val nc = if (code < 256) char.toString() else lut[code] ?: (c + c[0])
            result.append(nc)
            lut[n] = c + nc[0]
            n += 1
            c = nc
        }

        return result.toString()
    }

    private fun jsInt(x: Char): Int = if (x.isDigit()) x.toString().toInt() else 0

    private fun buildArray(encodedString: String): List<List<Int>> {
        val d = mutableListOf<List<Int>>()
        val c = encodedString.toMutableList()
        var count = jsInt(c.removeAt(0))

        while (count != 0) {
            val currentArray = mutableListOf<Int>()
            repeat(count) {
                currentArray.add(0, jsInt(c.removeAt(0)))
            }
            d.add(currentArray)
            count = jsInt(c.removeAt(0))
        }

        return d
    }

    private fun decodeUrl(etext: String, tarray: List<Int>): String {
        var ds = etext
        for (t in tarray) {
            if (t == 1) {
                ds = ds.reversed()
            }
            val bytes = ds.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            ds = bytes.toString(UTF_8)
            ds = ds.replace("dXRmOA==", "")
        }
        return ds
    }

    private interface Service {
        @GET
        suspend fun getWithHeaders(
            @Url url: String,
            @Header("Referer") referer: String,
            @Header("User-Agent") userAgent: String,
            @Header("Origin") origin: String
        ): ResponseBody

        companion object {
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