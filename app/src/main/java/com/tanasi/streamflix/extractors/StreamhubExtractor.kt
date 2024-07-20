package com.tanasi.streamflix.extractors

import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.models.Video
import com.tanasi.streamflix.utils.JsUnpacker
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url

class StreamhubExtractor : Extractor() {

    override val name = "Streamhub"
    override val mainUrl = "https://streamhub.to"

    override suspend fun extract(link: String): Video {
        val service = Service.build(mainUrl)

        val source = service.getSource(link.replace("streamhub.to/d/", "streamhub.to/e/"))

        val packedJS = Regex("(eval\\(function\\(p,a,c,k,e,d\\)(.|\\n)*?)</script>")
            .find(source.toString())?.let { it.groupValues[1] }
            ?: throw Exception("Packed JS not found")

        val unPacked = JsUnpacker(packedJS).unpack()
            ?: throw Exception("Unpacked is null")

        val sources = Regex("src:\"(.*?)\"")
            .findAll(Regex("\\{sources:\\[(.*?)]")
                .find(unPacked)?.let { it.groupValues[1] }
                ?: throw Exception("No sources found")
            )
            .map { it.groupValues[1] }.toList()

        val video = Video(
            source = sources.firstOrNull() ?: "",
            subtitles = source.select("video > track")
                .map {
                    Video.Subtitle(
                        label = it.attr("label"),
                        file = it.attr("src"),
                    )
                }
                .filter { it.label != "Upload SRT" }
                .sortedBy { it.label }
        )

        return video
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
        suspend fun getSource(@Url url: String): Document
    }
}