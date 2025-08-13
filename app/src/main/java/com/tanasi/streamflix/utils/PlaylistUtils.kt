package com.tanasi.streamflix.utils

import androidx.media3.common.MimeTypes
import com.tanasi.streamflix.models.Video
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

class PlaylistUtils(private val client: OkHttpClient) {

    fun extractFromHls(
        playlistUrl: String,
        referer: String? = null,
    ): List<Video> {
        val masterPlaylist = try {
            val request = Request.Builder().url(playlistUrl)
            if (referer != null) {
                request.header("Referer", referer)
            }
            client.newCall(request.build()).execute().body?.string() ?: ""
        } catch (e: Exception) {
            // Si la URL principal ya es un video, devuélvelo directamente
            return listOf(Video(source = playlistUrl, subtitles = emptyList(), type = MimeTypes.APPLICATION_M3U8))
        }

        val videoList = mutableListOf<Video>()

        if (!masterPlaylist.contains("#EXT-X-STREAM-INF")) {
            videoList.add(
                Video(
                    source = playlistUrl,
                    subtitles = emptyList(),
                    type = MimeTypes.APPLICATION_M3U8
                )
            )
            return videoList
        }

        val masterUrl = playlistUrl.toHttpUrl()
        val masterBaseUrl = masterUrl.toString().substringBeforeLast("/") + "/"

        // Obtener subtítulos de la playlist
        val subtitles = SUBTITLE_REGEX.findAll(masterPlaylist).map {
            val url = getAbsoluteUrl(it.groupValues[2], masterUrl.toString(), masterBaseUrl)
            Video.Subtitle(
                label = it.groupValues[1],
                file = url,
            )
        }.toList()

        masterPlaylist.substringAfter("#EXT-X-STREAM-INF:").split("#EXT-X-STREAM-INF:").forEach {
            val resolution = RESOLUTION_REGEX.find(it)?.groupValues?.get(1)
            val quality = resolution?.let { res -> "${res}p" } ?: getQualityFromBandwidth(it)
            val url = getAbsoluteUrl(it.substringAfter("\n").trim(), masterUrl.toString(), masterBaseUrl)
            val videoHeaders = referer?.let { mapOf("Referer" to it) }

            videoList.add(
                Video(
                    source = url,
                    subtitles = subtitles,
                    type = MimeTypes.APPLICATION_M3U8,
                    headers = videoHeaders
                )
            )
        }
        return videoList.sortedByDescending { it.source.contains("1080") }
    }

    private fun getAbsoluteUrl(url: String, playlistUrl: String, masterBase: String): String {
        return when {
            url.startsWith("http") -> url
            url.startsWith("//") -> "https:$url"
            url.startsWith("/") -> playlistUrl.toHttpUrl().scheme + "://" + playlistUrl.toHttpUrl().host + url
            else -> masterBase + url
        }
    }

    private fun getQualityFromBandwidth(text: String): String {
        val bandwidth = BANDWIDTH_REGEX.find(text)?.groupValues?.get(1)?.toIntOrNull() ?: return "Default"
        return when {
            bandwidth >= 2000000 -> "1080p"
            bandwidth >= 1000000 -> "720p"
            bandwidth >= 600000 -> "480p"
            else -> "360p"
        }
    }

    companion object {
        private val SUBTITLE_REGEX by lazy { Regex("""#EXT-X-MEDIA:TYPE=SUBTITLES.*?NAME="(.*?)".*?URI="(.*?)"""") }
        private val RESOLUTION_REGEX by lazy { Regex("""RESOLUTION=\d{3,4}x(\d{3,4})""") }
        private val BANDWIDTH_REGEX by lazy { Regex("""BANDWIDTH=(\d+)""") }
    }
}