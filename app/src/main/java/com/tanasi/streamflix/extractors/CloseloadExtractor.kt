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

        val unpacked = JsUnpacker(document.toString())
            .unpack()
            ?: throw Exception("Can't unpack JS")

        var source = Regex("=\"(aHR.*?)\";").find(unpacked)
            ?.groupValues?.get(1)
            ?.let { Base64.decode(it, Base64.DEFAULT).toString(Charsets.UTF_8) }
        if (source == null){
            val myPlayerSrc = Regex("""myPlayer\.src\(\{\s*src:\s*(\w+)\s*,""")
            val videoSrcVarName = myPlayerSrc.find(unpacked)
                ?.groupValues?.get(1)
                ?: throw Exception("Can't find variable name used in myPlayer.src")
            val encodedM3u8Regex = Regex("""var\s+$videoSrcVarName\s*=\s*dc_hello\("([^"]+)"\)""")
            val encodedLink = encodedM3u8Regex.find(unpacked)
                ?.groupValues?.get(1)
                ?:""
            if (!encodedLink.equals("")){
                val decodedLink = String(Base64.decode(encodedLink, Base64.DEFAULT))
                val decodedLinkReversed = decodedLink.reversed()
                val finalDecodedLink = String(Base64.decode(decodedLinkReversed, Base64.DEFAULT))
                val urlRegex = Regex("""https://[^\s"]+""")
                source = urlRegex.find(finalDecodedLink)?.value.toString()
            } else {
                val playerSrcRegex = Regex("""myPlayer\.src\(\{\s*src:\s*(\w+)\s*,""")
                val playerSrcVar = playerSrcRegex.find(unpacked)?.groupValues?.get(1)
                    ?: error("Couldn't find myPlayer.src variable")

                val arrayDecoderRegex = Regex("""var\s+$playerSrcVar\s*=\s*(\w+)\(\[((?:\s*"[^"]+",?)+)\]\)""")
                val match = arrayDecoderRegex.find(unpacked)
                    ?: error("Couldn't match the decoding function with array")

                val arrayParts = Regex("\"([^\"]+)\"").findAll(match.groupValues[2]).map { it.groupValues[1] }.toList()

                val decodedUrl = decodeObfuscatedUrl(arrayParts)
                source = decodedUrl
            }

        }

        return Video(
            source = source,
            headers = mapOf(
                "Referer" to mainUrl,
            ),
            type = MimeTypes.APPLICATION_M3U8,
        )
    }
    private fun decodeObfuscatedUrl(parts: List<String>): String {
        val joined = parts.joinToString("")

        val rot13 = joined.map {
            when (it) {
                in 'A'..'Z' -> 'A' + (it - 'A' + 13) % 26
                in 'a'..'z' -> 'a' + (it - 'a' + 13) % 26
                else -> it
            }
        }.joinToString("")

        val base64Decoded = Base64.decode(rot13, Base64.DEFAULT)
        val reversed = base64Decoded.reversed()

        val finalBytes = reversed.mapIndexed { i, b ->
            val adjusted = (b.toInt() - (399756995 % (i + 5)) + 256) % 256
            adjusted.toByte()
        }.toByteArray()

        return String(finalBytes)
    }


    private interface Service {

        companion object {
            fun build(baseUrl: String): Service {
                val client = OkHttpClient.Builder()
                    .build()

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
        suspend fun get(
            @Url url: String,
            @Header("referer") referer: String,
        ): Document
    }
}