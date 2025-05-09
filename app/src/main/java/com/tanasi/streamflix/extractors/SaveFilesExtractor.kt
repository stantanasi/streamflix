package com.tanasi.streamflix.extractors

import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.models.Video
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url

class SaveFilesExtractor: Extractor() {

    override val name = "Savefiles"
    override val mainUrl = "https://savefiles.com/"


    override suspend fun extract(link: String): Video {
        val service = SaveFilesExtractorService.build(mainUrl)
        val source = service.getSource(link.replace(mainUrl, ""))
        val scriptTags = source.select("script[type=text/javascript]")

        var m3u8: String? = null

        for (script in scriptTags) {
            val scriptData = script.data()
            if ("jwplayer" in scriptData && "sources" in scriptData && "file" in scriptData) {
                val fileRegex = Regex("""file\s*:\s*["']([^"']+)["']""")
                val match = fileRegex.find(scriptData)
                if (match != null) {
                    m3u8 = match.groupValues[1]
                    break
                }
            }
        }

        return Video(
            source = m3u8.toString(),
            subtitles = listOf()
        )

    }

    private interface SaveFilesExtractorService {
        companion object {
            fun build(baseUrl: String): SaveFilesExtractorService {
                val retrofitRedirected = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .build()
                return retrofitRedirected.create(SaveFilesExtractorService::class.java)
            }
        }
        @GET
        suspend fun getSource(@Url url: String): Document
    }
}