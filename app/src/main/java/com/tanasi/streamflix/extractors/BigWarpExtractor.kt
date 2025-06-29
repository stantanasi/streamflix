package com.tanasi.streamflix.extractors

import androidx.media3.common.MimeTypes
import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.models.Video
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url
class BigWarpExtractor: Extractor() {

    override val name = "BigWarp (VLC only)"
    override val mainUrl = "https://bigwarp.cc/"


    override suspend fun extract(link: String): Video {
        val service = BigWarpExtractorService.build(mainUrl)
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
            subtitles = listOf(),
            type = MimeTypes.APPLICATION_MP4,
            headers =                     mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
                "Referer" to mainUrl
            )
        )

    }


    private interface BigWarpExtractorService {

        companion object {
            fun build(baseUrl: String): BigWarpExtractorService {
                val retrofitRedirected = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .build()
                return retrofitRedirected.create(BigWarpExtractorService::class.java)
            }
        }



        @GET
        suspend fun getSource(@Url url: String): Document
    }


}
