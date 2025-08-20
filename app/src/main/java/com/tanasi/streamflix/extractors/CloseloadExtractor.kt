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
            ?: extractArrayDecodedNew(unpacked)
            ?: error("Unable to fetch video URL")

        return Video(
            source = source,
            headers = mapOf("Referer" to mainUrl),
            type = MimeTypes.APPLICATION_M3U8
        )
    }

    private fun safeBase64Decode(str: String): ByteArray? = try {
        Base64.decode(str, Base64.DEFAULT)
    } catch (e: IllegalArgumentException) {
        null
    }

    private fun rot13(input: String): String = input.map {
        when (it) {
            in 'A'..'Z' -> 'A' + (it - 'A' + 13) % 26
            in 'a'..'z' -> 'a' + (it - 'a' + 13) % 26
            else -> it
        }
    }.joinToString("")

    private fun extractDirectBase64(unpacked: String): String? {
        val match = Regex("=\"(aHR.*?)\";").find(unpacked)?.groupValues?.get(1) ?: return null
        val decoded = safeBase64Decode(match) ?: return null
        val url = String(decoded, Charsets.UTF_8)
        return url.takeIf { it.startsWith("http") }
    }

    private fun extractDcHelloEncoded(unpacked: String): String? {
        val varName = Regex("""myPlayer\.src\(\{\s*src:\s*(\w+)\s*,""")
            .find(unpacked)?.groupValues?.get(1) ?: return null

        val encodedLink = Regex("""var\s+$varName\s*=\s*dc_hello\("([^"]+)"\)""")
            .find(unpacked)?.groupValues?.get(1).orEmpty()

        if (encodedLink.isBlank()) return null

        val step1 = safeBase64Decode(encodedLink)?.toString(Charsets.UTF_8) ?: return null
        val step2 = step1.reversed()
        val step3 = safeBase64Decode(step2)?.toString(Charsets.UTF_8) ?: return null

        return Regex("""https://[^\s"]+""").find(step3)?.value
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

        val candidates = listOfNotNull(
            decodeObfuscatedUrlRotFirst(arrayParts),
            decodeObfuscatedUrl(arrayParts),
            decodeObfuscatedUrlDecodeFirst(arrayParts),
            decodeObfuscatedUrlReverseFirst(arrayParts) // <-- new fallback
        )
        return candidates.firstOrNull { it.startsWith("http") }
    }

    private fun extractArrayDecodedNew(unpacked: String): String? {
        val varName = Regex("""myPlayer\.src\(\{\s*src:\s*(\w+)\s*,""")
            .find(unpacked)?.groupValues?.get(1) ?: return null

        val match = Regex("""var\s+$varName\s*=\s*(\w+)\(\[((?:\s*"[^"]+",?)+)\]\)""")
            .find(unpacked) ?: return null

        val arrayParts = Regex("\"([^\"]+)\"")
            .findAll(match.groupValues[2])
            .map { it.groupValues[1] }
            .toList()

        return decodeObfuscatedUrl(arrayParts)?.takeIf { it.startsWith("http") }
    }

    private fun decodeObfuscatedUrl(parts: List<String>): String? {
        val joined = parts.joinToString("")
        val rot = rot13(joined)
        val reversed = rot.reversed()
        val decoded = safeBase64Decode(reversed) ?: return null

        val finalBytes = ByteArray(decoded.size) { i ->
            val b = decoded[i]
            val adjustment = 399_756_995 % (i + 5)
            ((b.toInt() - adjustment + 256) % 256).toByte()
        }
        return String(finalBytes, Charsets.UTF_8)
    }

    private fun decodeObfuscatedUrlRotFirst(parts: List<String>): String? {
        val rot = rot13(parts.joinToString(""))
        val decoded = safeBase64Decode(rot) ?: return null

        val finalBytes = ByteArray(decoded.size) { i ->
            val b = decoded[i]
            val adjustment = 399_756_995 % (i + 5)
            ((b.toInt() - adjustment + 256) % 256).toByte()
        }
        return String(finalBytes, Charsets.UTF_8)
    }

    private fun decodeObfuscatedUrlDecodeFirst(parts: List<String>): String? {
        val b64 = safeBase64Decode(parts.joinToString("")) ?: return null
        val reversed = String(b64, Charsets.ISO_8859_1).reversed()
        val rot = rot13(reversed)

        val finalBytes = ByteArray(rot.length) { i ->
            val code = rot[i].code and 0xFF
            val adj = 399_756_995 % (i + 5)
            (((code - adj) % 256 + 256) % 256).toByte()
        }
        return String(finalBytes, Charsets.UTF_8)
    }

    private fun decodeObfuscatedUrlReverseFirst(parts: List<String>): String? {
        val joined = parts.joinToString("")
        val reversed = joined.reversed()
        val b64Decoded = safeBase64Decode(reversed) ?: return null

        val rot13Bytes = b64Decoded.map { b ->
            val c = b.toInt().toChar()
            when (c) {
                in 'A'..'Z' -> (((c - 'A' + 13) % 26) + 'A'.code).toByte()
                in 'a'..'z' -> (((c - 'a' + 13) % 26) + 'a'.code).toByte()
                else -> b
            }
        }.toByteArray()

        val finalBytes = ByteArray(rot13Bytes.size) { i ->
            val adjustment = 399_756_995 % (i + 5)
            ((rot13Bytes[i].toInt() - adjustment + 256) % 256).toByte()
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
