package com.tanasi.streamflix.extractors

import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.models.Video
import com.tanasi.streamflix.providers.RidomoviesProvider.URL
import com.tanasi.streamflix.utils.JsUnpacker
import com.tanasi.streamflix.utils.UserPreferences
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url
import java.net.URL

class VidHideExtractor: Extractor() {
    override val name = "VidHide"
    override val mainUrl = "https://dhtpre.com"
    override val aliasUrls= listOf("https://peytonepre.com")
    companion object {
        const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/115.0"
    }
    override suspend fun extract(link: String): Video {
        val mainLink = URL(link).protocol + "://" + URL(link).host
        val service = Service.build(mainLink)

        val source = service.getSource(link,  UserPreferences.currentProvider!!.baseUrl, UserPreferences.currentProvider!!.baseUrl, DEFAULT_USER_AGENT)
        val packedJS = Regex("(eval\\(function\\(p,a,c,k,e,d\\)(.|\\n)*?)</script>")
            .find(source.toString())?.let { it.groupValues[1] }
            ?: throw Exception("Packed JS not found")
        val unPacked = JsUnpacker(packedJS).unpack()
            ?: throw Exception("Unpacked is null")
        val links = mutableMapOf<String, String>()
        Regex("""["'](hls\d+)["']\s*:\s*["'](.*?)["']""")
            .findAll(unPacked)
            .forEach {
                links[it.groupValues[1]] = it.groupValues[2]
            }
        val finalUrl = links["hls4"] ?: links["hls2"] ?: throw Exception("No HLS link found")

        return Video(
            source = finalUrl
        )
    }




    private interface Service {

        companion object {
            fun build(baseUrl: String): Service {
                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .build()

                return retrofit.create(Service::class.java)
            }
        }

        @GET

        suspend fun getSource(@Url url: String,
                              @Header("Referer") referer: String,
                              @Header("Origin") origin: String,
                              @Header("User-Agent") userAgent: String = DEFAULT_USER_AGENT
        ): Document
    }
}