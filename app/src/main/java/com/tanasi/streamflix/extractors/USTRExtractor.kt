package com.tanasi.streamflix.extractors

import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.models.Video
import com.tanasi.streamflix.utils.JsUnpacker
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url

class USTRExtractor: Extractor() {
    override val name = "USTR"
    override val mainUrl = "https://ups2up.fun"

    override suspend fun extract(link: String): Video {
        val service = Service.build(mainUrl)
        val linkJustParameter = link.replace(mainUrl, "")

        val document = service.getSource(linkJustParameter)
        val packedJS = Regex("(eval\\(function\\(p,a,c,k,e,d\\)(.|\\n)*?)</script>")
            .find(document.toString())?.let { it.groupValues[1] }
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


    private interface Service {
        @GET
        suspend fun getSource(@Url url: String): Document

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