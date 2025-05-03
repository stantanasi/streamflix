package com.tanasi.streamflix.extractors

import android.util.Base64
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.models.Video
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url

class VoeExtractor : Extractor() {

    override val name = "VOE"
    override val mainUrl = "https://voe.sx/"

    val encodedRegex = Regex("""MKGMa="(.*?)";""", RegexOption.DOT_MATCHES_ALL)

    override suspend fun extract(link: String): Video {
        val service = VoeExtractorService.build(mainUrl, link)
        val source = service.getSource(link.replace(mainUrl, ""))
        val scriptTag = source.selectFirst("script[type=application/json]")
        val jsonContent = scriptTag?.data()?.trim()
        var m3u8 = "";
        val encodedString = encodedRegex.find(jsonContent ?: "")?.groupValues?.getOrNull(1)
        if (encodedString != null) {
            val decryptedJson = decryptF7(jsonContent.toString())
            m3u8 = decryptedJson.get("source")?.asString.toString()
        } else {
            val decryptedJson = decryptF7(jsonContent.toString())
            m3u8 = decryptedJson.get("source")?.asString.toString()
        }


        return Video(
            source = m3u8,
            subtitles = listOf()
        )

    }

    private fun decryptF7(p8: String): JsonObject {
        return try {
            val vF = rot13(p8)
            val vF2 = replacePatterns(vF)
            val vF3 = removeUnderscores(vF2)
            val vF4 = Base64.decode(vF3, Base64.NO_WRAP).toString(Charsets.UTF_8)
            val vF5 = charShift(vF4, 3)
            val vF6 = reverse(vF5)
            val vAtob = Base64.decode(vF6, Base64.NO_WRAP).toString(Charsets.UTF_8)

            JsonParser.parseString(vAtob).asJsonObject
        } catch (e: Exception) {
            println("Decryption error: ${e.message}")
            JsonObject()
        }
    }

    private fun rot13(input: String): String {
        return input.map { c ->
            when (c) {
                in 'A'..'Z' -> ((c - 'A' + 13) % 26 + 'A'.code).toChar()
                in 'a'..'z' -> ((c - 'a' + 13) % 26 + 'a'.code).toChar()
                else -> c
            }
        }.joinToString("")
    }

    private fun replacePatterns(input: String): String {
        val patterns = listOf("@$", "^^", "~@", "%?", "*~", "!!", "#&")
        return patterns.fold(input) { result, pattern ->
            result.replace(Regex(Regex.escape(pattern)), "_")
        }
    }

    private fun removeUnderscores(input: String): String = input.replace("_", "")

    private fun charShift(input: String, shift: Int): String {
        return input.map { (it.code - shift).toChar() }.joinToString("")
    }

    private fun reverse(input: String): String = input.reversed()


    private interface VoeExtractorService {

        companion object {
            suspend fun build(baseUrl: String, originalLink: String): VoeExtractorService {
                val retrofitVOE = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .build()
                val retrofitVOEBuiled = retrofitVOE.create(VoeExtractorService::class.java)

                val retrofitVOEhtml =
                    retrofitVOEBuiled.getSource(originalLink.replace(baseUrl, "")).html()
                val redirectBaseUrl =
                    "https://" + retrofitVOEhtml.split("https://")[1].split("e/")[0]
                val retrofitRedirected = Retrofit.Builder()
                    .baseUrl(redirectBaseUrl)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .build()
                return retrofitRedirected.create(VoeExtractorService::class.java)
            }
        }

        @GET
        suspend fun getSource(@Url url: String): Document
    }
}