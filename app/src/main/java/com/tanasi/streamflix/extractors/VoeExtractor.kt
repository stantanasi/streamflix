package com.tanasi.streamflix.extractors

import android.util.Base64
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.models.Video
import com.tanasi.streamflix.utils.DecryptHelper
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url

class VoeExtractor : Extractor() {

    override val name = "VOE"
    override val mainUrl = "https://voe.sx/"


    override suspend fun extract(link: String): Video {
        val service = VoeExtractorService.build(mainUrl, link)
        val source = service.getSource(link.replace(mainUrl, ""))
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
