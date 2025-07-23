package com.tanasi.streamflix.extractors

import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.models.Video
import com.tanasi.streamflix.utils.JsUnpacker
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url

open class StreamWishExtractor : Extractor() {

    override val name = "Streamwish"
    override val mainUrl = "https://streamwish.to"
    override val aliasUrls = listOf("https://streamwish.com","https://streamwish.to","https://ajmidyad.sbs","https://khadhnayad.sbs","https://yadmalik.sbs",
        "https://hayaatieadhab.sbs","https://kharabnahs.sbs","https://atabkhha.sbs","https://atabknha.sbs","https://atabknhk.sbs",
        "https://atabknhs.sbs","https://abkrzkr.sbs","https://abkrzkz.sbs","https://wishembed.pro","https://mwish.pro","https://strmwis.xyz",
        "https://awish.pro","https://dwish.pro","https://vidmoviesb.xyz","https://embedwish.com","https://cilootv.store","https://uqloads.xyz",
        "https://tuktukcinema.store","https://doodporn.xyz","https://ankrzkz.sbs","https://volvovideo.top","https://streamwish.site",
        "https://wishfast.top","https://ankrznm.sbs","https://sfastwish.com","https://eghjrutf.sbs","https://eghzrutw.sbs",
        "https://playembed.online","https://egsyxurh.sbs","https://egtpgrvh.sbs","https://flaswish.com","https://obeywish.com",
        "https://cdnwish.com","https://javsw.me","https://cinemathek.online","https://trgsfjll.sbs","https://fsdcmo.sbs",
        "https://anime4low.sbs","https://mohahhda.site","https://ma2d.store","https://dancima.shop","https://swhoi.com",
        "https://gsfqzmqu.sbs","https://jodwish.com","https://swdyu.com","https://strwish.com","https://asnwish.com",
        "https://wishonly.site","https://playerwish.com","https://katomen.store","https://hlswish.com","https://streamwish.fun",
        "https://swishsrv.com","https://iplayerhls.com","https://hlsflast.com","https://4yftwvrdz7.sbs","https://ghbrisk.com",
        "https://eb8gfmjn71.sbs","https://cybervynx.com","https://edbrdl7pab.sbs","https://stbhg.click","https://dhcplay.com","https://gradehgplus.com", "https://ultpreplayer.com")

    protected var referer = ""

    override suspend fun extract(link: String): Video {
        val service = Service.build(mainUrl)

        val document = service.get(link, referer = referer)


        val script = Regex("<script .*>(eval.*?)</script>", RegexOption.DOT_MATCHES_ALL).find(document.toString())
            ?.groupValues?.get(1)
            ?.let { JsUnpacker(it).unpack() }
            ?: throw Exception("Can't retrieve script")

        val source = Regex("\"hls(\\d+)\"\\s*:\\s*\"(https:[^\"]+\\.m3u8[^\"]*)\"")
            .findAll(script)
            .map { it.groupValues[1].toInt() to it.groupValues[2] }
            .sortedBy { it.first }  // hls2 > hls3 > hls4
            .map { it.second }
            .firstOrNull()
            ?: throw Exception("Can't retrieve m3u8")

        val subtitles = Regex("file:\\s*\"(.*?)\"(?:,label:\\s*\"(.*?)\")?,kind:\\s*\"(.*?)\"").findAll(
            Regex("tracks:\\s*\\[(.*?)]").find(script)
                ?.groupValues?.get(1)
                ?: ""
        )
            .filter { it.groupValues[3] == "captions" }
            .map {
                Video.Subtitle(
                    label = it.groupValues[2],
                    file = it.groupValues[1],
                )
            }
            .toList()

        val video = Video(
            source = source,
            subtitles = subtitles,
            headers = mapOf(
                "Referer" to referer,
                "Origin" to mainUrl,
                "User-Agent" to "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:139.0) Gecko/20100101 Firefox/139.0",
                "Accept" to "*/*",
                "Accept-Language" to "en-US,en;q=0.5",
                "Connection" to "keep-alive",
            ),
        )

        return video
    }


    class UqloadsXyz : StreamWishExtractor() {
        override val name = "Uqloads"
        override val mainUrl = "https://uqloads.xyz"

        suspend fun extract(link: String, referer: String): Video {
            this.referer = referer
            return extract(link)
        }
    }
    class SwiftPlayersExtractor : StreamWishExtractor(){
        override val name = "SwiftPlayer"
        override val mainUrl = "https://swiftplayers.com/"
    }

    class SwishExtractor : StreamWishExtractor() {
        override val name = "Swish"
        override val mainUrl = "https://swishsrv.com/"
    }

    class HlswishExtractor : StreamWishExtractor() {
        override val name = "Hlswish"
        override val mainUrl = "https://hlswish.com/"
    }

    class PlayerwishExtractor : StreamWishExtractor() {
        override val name = "Playerwish"
        override val mainUrl = "https://playerwish.com/"
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
        suspend fun get(
            @Url url: String,
            @Header("referer") referer: String = "",
        ): Document
    }
}