package com.tanasi.streamflix.extractors

import android.util.Base64
import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.models.Video
import com.tanasi.streamflix.utils.OpenSubtitles
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url

class VidsrcNetExtractor : Extractor() {

    override val name = "Vidsrc.net"
    override val mainUrl = "https://vidsrc.net"

    fun server(videoType: Video.Type): Video.Server {
        return Video.Server(
            id = name,
            name = name,
            src = when (videoType) {
                is Video.Type.Episode -> "$mainUrl/embed/tv?tmdb=${videoType.tvShow.id}&season=${videoType.season.number}&episode=${videoType.number}"
                is Video.Type.Movie -> "$mainUrl/embed/movie?tmdb=${videoType.id}"
            },
        )
    }

    override suspend fun extract(link: String): Video {
        val service = Service.build(mainUrl)

        val iframedoc = service.get(link)
            .select("iframe#player_iframe").attr("src")
            .let { if (it.startsWith("//")) "https:$it" else it }

        val doc = service.get(iframedoc, referer = link)

        val imdbId = doc.select("body").attr("data-i").toIntOrNull()
        val srcrcp = Regex("src: '(//vidsrc\\.net/srcrcp/.*?)'")
            .find(doc.toString())?.groupValues?.get(1)
            ?: throw Exception("Can't retrieve source")

        val script = service.get(
            if (srcrcp.startsWith("//")) "https:$srcrcp" else srcrcp,
            referer = iframedoc
        ).toString()

        val playerId = Regex("Playerjs.*file: ([a-zA-Z0-9]*?) ,")
            .find(script)?.groupValues?.get(1)
            ?: throw Exception("Can't retrieve player ID")

        val encryptedSource = Regex("""<div id="$playerId" style="display:none;">(.*?)</div>""")
            .find(script)?.groupValues?.get(1)
            ?: throw Exception("Can't retrieve source")

        return Video(
            source = decrypt(encryptedSource),
            subtitles = if (imdbId != null) {
                listOf(
                    OpenSubtitles.search(imdbId, "eng").sortedBy { it.subDownloadsCnt },
                    OpenSubtitles.search(imdbId, "fre").sortedBy { it.subDownloadsCnt },
                    OpenSubtitles.search(imdbId, "ger").sortedBy { it.subDownloadsCnt },
                    OpenSubtitles.search(imdbId, "por").sortedBy { it.subDownloadsCnt },
                ).flatten().map {
                    Video.Subtitle(
                        label = it.languageName ?: it.subFileName ?: "",
                        file = "https://vidsrc.stream/sub/ops-${it.idSubtitleFile}.vtt",
                    )
                }
            } else emptyList(),
            referer = iframedoc,
        )
    }

    private fun decrypt(encrypted: String): String {
        val shift = 3
        return encrypted.map { char ->
            when (char) {
                in 'a'..'z' -> {
                    val shifted = char - shift
                    if (shifted < 'a') shifted + 26 else shifted
                }
                in 'A'..'Z' -> {
                    val shifted = char - shift
                    if (shifted < 'A') shifted + 26 else shifted
                }
                else -> char
            }
        }.joinToString("")
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