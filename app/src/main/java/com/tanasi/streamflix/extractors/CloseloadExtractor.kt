package com.tanasi.streamflix.extractors

import android.util.Base64
import androidx.media3.common.MimeTypes
import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.models.Video
import com.tanasi.streamflix.providers.RidomoviesProvider
import com.tanasi.streamflix.utils.JsUnpacker
import okhttp3.OkHttpClient
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url

class CloseloadExtractor : Extractor() {

    override val name = "Closeload"
    override val mainUrl = "https://closeload.top/"

    override suspend fun extract(link: String): Video {
        val service = Service.build(mainUrl)
        val document = service.get(link, RidomoviesProvider.URL)

        val unpacked = JsUnpacker(document.toString()).unpack()
            ?: error("Can't unpack JS")

        val source = extractDirectBase64(unpacked)
            ?: extractDcHelloEncoded(unpacked)
            ?: extractArrayDecoded(unpacked)
            ?: error("Unable to fetch video URL")

        return Video(
            source = source,
            headers = mapOf("Referer" to mainUrl),
            type = MimeTypes.APPLICATION_M3U8
        )
    }
    private fun extractDirectBase64(unpacked: String): String? {
        val match = Regex("=\"(aHR.*?)\";").find(unpacked)?.groupValues?.get(1) ?: return null
        return Base64.decode(match, Base64.DEFAULT)
            .toString(Charsets.UTF_8)
            .takeIf { it.startsWith("http") }
    }

    private fun extractDcHelloEncoded(unpacked: String): String? {
        val varName = Regex("""myPlayer\.src\(\{\s*src:\s*(\w+)\s*,""")
            .find(unpacked)?.groupValues?.get(1) ?: return null

        val encodedLink = Regex("""var\s+$varName\s*=\s*dc_hello\("([^"]+)"\)""")
            .find(unpacked)?.groupValues?.get(1).orEmpty()

        if (encodedLink.isBlank()) return null

        val decodedLink = Base64.decode(encodedLink, Base64.DEFAULT).toString(Charsets.UTF_8)
        val reversed = decodedLink.reversed()
        val finalDecoded = Base64.decode(reversed, Base64.DEFAULT).toString(Charsets.UTF_8)

        return Regex("""https://[^\s"]+""").find(finalDecoded)?.value
    }
    private fun extractArrayDecoded(unpacked: String): String? {
        val varName = Regex("""myPlayer\.src\(\{\s*src:\s*(\w+)\s*,""")
            .find(unpacked)?.groupValues?.get(1) ?: return null

        val match = Regex("""var\s+$varName\s*=\s*(\w+)\(\[((?:\s*"[^"]+",?)+)\]\)""")
            .find(unpacked) ?: return null

        val arrayParts = Regex("\"([^\"]+)\"")
            .findAll(match.groupValues[2])
            .map { it.groupValues[1] }
            .toList()

        return decodeObfuscatedUrlRotFirst(arrayParts)
            .takeIf { it.startsWith("http") }
            ?: decodeObfuscatedUrlDecodeFirst(arrayParts)
                .takeIf { it.startsWith("http") }
    }

    private fun decodeObfuscatedUrlRotFirst(parts: List<String>): String {
        val rot13 = parts.joinToString("").map {
            when (it) {
                in 'A'..'Z' -> 'A' + (it - 'A' + 13) % 26
                in 'a'..'z' -> 'a' + (it - 'a' + 13) % 26
                else -> it
            }
        }.joinToString("")

        val reversed = Base64.decode(rot13, Base64.DEFAULT).reversed()
        val finalBytes = reversed.mapIndexed { i, b ->
            val adjusted = (b.toInt() - (399_756_995 % (i + 5)) + 256) % 256
            adjusted.toByte()
        }.toByteArray()

        return String(finalBytes, Charsets.UTF_8)
    }

    private fun decodeObfuscatedUrlDecodeFirst(parts: List<String>): String {
        val b64 = Base64.decode(parts.joinToString(""), Base64.DEFAULT)
        val reversed = String(b64, Charsets.ISO_8859_1).reversed()
        val rot13 = reversed.map {
            when (it) {
                in 'A'..'Z' -> 'A' + (it - 'A' + 13) % 26
                in 'a'..'z' -> 'a' + (it - 'a' + 13) % 26
                else -> it
            }
        }.joinToString("")

        val finalBytes = ByteArray(rot13.length) { i ->
            val code = rot13[i].code and 0xFF
            val adj = 399_756_995 % (i + 5)
            (((code - adj) % 256 + 256) % 256).toByte()
        }

        return String(finalBytes, Charsets.UTF_8)
    }

    private interface Service {
        companion object {
            fun build(baseUrl: String): Service {
                val client = OkHttpClient.Builder().build()
                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build()
                return retrofit.create(Service::class.java)
            }
        }

        @GET
        suspend fun get(@Url url: String, @Header("referer") referer: String): Document
    }
}
