package com.tanasi.streamflix.extractors

import android.util.Base64
import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.models.Video
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url

object SoraExtractor {

    private val service = Service.build()

    suspend fun invokeVidSrc(videoType: Video.Type): Video.Server? {
        val vidSrcAPI = "https://vidsrc.net"

        val url = when (videoType) {
            is Video.Type.Episode -> "$vidSrcAPI/embed/tv?tmdb=${videoType.tvShow.id}&season=${videoType.season.number}&episode=${videoType.number}"
            is Video.Type.Movie -> "$vidSrcAPI/embed/movie?tmdb=${videoType.id}"
        }

        val iframedoc = service.get(url)
            .select("iframe#player_iframe").attr("src")
            .let { if (it.startsWith("//")) "https:$it" else it }

        val doc = service.get(iframedoc, referer = url)

        val index = doc.select("body").attr("data-i")
        val hash = doc.select("div#hidden").attr("data-h")
        val srcrcp = deobfstr(hash, index)

        val script = service.get(
            if (srcrcp.startsWith("//")) "https:$srcrcp" else srcrcp,
            referer = iframedoc
        ).selectFirst("script:containsData(Playerjs)")?.data()

        val source = script?.substringAfter("file:\"#9")?.substringBefore("\"")
            ?.replace(Regex("/@#@\\S+?=?="), "")
            ?.let { Base64.decode(it, Base64.NO_WRAP).toString(Charsets.UTF_8) }
            ?: return null

        return Video.Server(
            id = "vidsrc",
            name = "Vidsrc",
        ).apply {
            video = Video(
                source = source,
                subtitles = emptyList(),
            )
        }
    }


    private fun deobfstr(hash: String, index: String): String {
        var result = ""
        for (i in hash.indices step 2) {
            val j = hash.substring(i, i + 2)
            result += (j.toInt(16) xor index[(i / 2) % index.length].code).toChar()
        }
        return result
    }


    private interface Service {

        companion object {
            fun build(): Service {
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://api.themoviedb.org/3/")
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