//package com.tanasi.streamflix.extractors
//
//import androidx.media3.common.MimeTypes
//import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
//import com.tanasi.streamflix.models.Video
//import org.jsoup.nodes.Document
//import retrofit2.Retrofit
//import retrofit2.http.GET
//import retrofit2.http.Url
// TODO exoplayer does not seem to play the mp4 from this video provider
//class BigWarpExtractor: Extractor() {
//
//    override val name = "BigWarp"
//    override val mainUrl = "https://bigwarp.art/"
//
//
//    override suspend fun extract(link: String): Video {
//        val service = SaveFilesExtractorService.build(mainUrl)
//        val source = service.getSource(link.replace(mainUrl, ""))
//        val scriptTags = source.select("script[type=text/javascript]")
//
//        var m3u8: String? = null
//
//        for (script in scriptTags) {
//            val scriptData = script.data()
//            if ("jwplayer" in scriptData && "sources" in scriptData && "file" in scriptData) {
//                val fileRegex = Regex("""file\s*:\s*["']([^"']+)["']""")
//                val match = fileRegex.find(scriptData)
//                if (match != null) {
//                    m3u8 = match.groupValues[1]
//                    break
//                }
//            }
//        }
//
//        return Video(
//            source = m3u8.toString(),
//            subtitles = listOf(),
//            type = MimeTypes.APPLICATION_MP4,
//            headers =                     mapOf(
//                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
//                "Referer" to mainUrl
//            )
//        )
//
//    }
//
//
//    private interface SaveFilesExtractorService {
//
//        companion object {
//            fun build(baseUrl: String): SaveFilesExtractorService {
//                val retrofitRedirected = Retrofit.Builder()
//                    .baseUrl(baseUrl)
//                    .addConverterFactory(JsoupConverterFactory.create())
//                    .build()
//                return retrofitRedirected.create(SaveFilesExtractorService::class.java)
//            }
//        }
//
//
//
//        @GET
//        suspend fun getSource(@Url url: String): Document
//    }
//
//
////    <script type='text/javascript'>
////    jwplayer("vplayer").setup({
////        sources: [{file:"https://fs71.bigwarp.io/v/01/00460/l4jjyb74vi4b_x/x.mp4?t=cmf0akHA84gm5s0pP7zR_XinSDW-MKV_MIBBsEcH2-8&s=1746805282&e=43200&f=2301408&sp=1000&i=0.0&kmnr=176803424",label:"1920x1080 1511 kbps"}]
//    }
