package com.tanasi.streamflix.extractors

import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.models.Video
import com.tanasi.streamflix.utils.JsUnpacker
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url


open class FilemoonExtractor : Extractor() {

    override val name = "Filemoon"
    override val mainUrl = "https://filemoon.site"

    override suspend fun extract(link: String): Video {
        val service = Service.build(mainUrl)
        val linkJustParameter = link.replace(mainUrl, "")

        val document = service.getSource(linkJustParameter)
        val iframeSrc = document.selectFirst("#iframe-holder iframe")?.attr("src")
            ?: throw Exception("Iframe not found")

        val iframeUrl = if (iframeSrc.startsWith("http")) iframeSrc else "https:$iframeSrc"
        val source = service.getWithHeaders(
            iframeUrl,
            referer = link
        )
        val packedJS = Regex("(eval\\(function\\(p,a,c,k,e,d\\)(.|\\n)*?)</script>")
            .find(source.toString())?.let { it.groupValues[1] }
            ?: throw Exception("Packed JS not found")
        val unPacked = JsUnpacker(packedJS).unpack()
            ?: throw Exception("Unpacked is null")
        val sources = Regex("""file:"(.*?)"""")
            .findAll(
                Regex("""sources:\[(.*?)]""")
                    .find(unPacked)?.groupValues?.get(1)
                    ?: throw Exception("No sources found")
            )
            .map { it.groupValues[1] }
            .toList()


        return Video(
            source = sources.firstOrNull() ?: "",
        )
    }

    class Any(hostUrl: String) : FilemoonExtractor() {
        override val mainUrl = hostUrl
    }

    private interface Service {
        @GET
        suspend fun getSource(@Url url: String): Document

        @GET
        suspend fun getWithHeaders(
            @Url url: String,
            @Header("Referer") referer: String,
            @Header("User-Agent") userAgent: String = DEFAULT_USER_AGENT
        ): Document

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