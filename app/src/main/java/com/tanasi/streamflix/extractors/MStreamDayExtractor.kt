package com.tanasi.streamflix.extractors

import android.util.Base64
import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.models.Video
import com.tanasi.streamflix.utils.AADecoder
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url

// based on:
// https://github.com/doGior/doGiorsHadEnough/blob/master/AnimeWorld/src/main/kotlin/it/dogior/hadEnough/VidguardExtractor.kt
// https://github.com/Gujal00/ResolveURL/blob/master/script.module.resolveurl/lib/resolveurl/plugins/vidguard.py
class MStreamDayExtractor : Extractor() {
    override val name = Base64.decode(
        "bW9mbGl4", Base64.NO_WRAP
    ).toString(Charsets.UTF_8) + Base64.decode(
        "LXN0cmVhbS5kYXk=", Base64.NO_WRAP
    ).toString(Charsets.UTF_8)
    override val mainUrl = Base64.decode(
        "aHR0cHM6Ly9tb2ZsaXg=", Base64.NO_WRAP
    ).toString(Charsets.UTF_8) + Base64.decode(
        "LXN0cmVhbS5kYXk=", Base64.NO_WRAP
    ).toString(Charsets.UTF_8)

    override suspend fun extract(link: String): Video {
        val service = MStreamDayExtractorService.build(mainUrl, link)
        val source = service.getSource(link.replace(mainUrl, ""))
        val html = source.html()
        var encodedSource = html.split("window.ADBLOCKER = false;\\n")[1].split("\");</script>")[0]

        encodedSource = encodedSource.replace("\\u002b", "+")
        encodedSource = encodedSource.replace("\\u0027", "'")
        encodedSource = encodedSource.replace("\\u0022", "\"")
        encodedSource = encodedSource.replace("\\/", "/")
        encodedSource = encodedSource.replace("\\\\", "\\")
        encodedSource = encodedSource.replace("\\\"", "\"")

        var decodedSoure = AADecoder.decode(encodedSource, true)
        val urlEncoded = decodedSoure.split("window.svg={\"stream\":\"")[1].split("\",\"hash")[0]

        val urlSigDecoded = sigDecode(urlEncoded)
        return Video(source = urlSigDecoded)
    }

    private fun sigDecode(url: String): String {
        val sig = url.split("sig=")[1].split("&")[0]
        val sigChunkedXOR =
            sig.chunked(2).joinToString("") { (Integer.parseInt(it, 16) xor 2).toChar().toString() }
        val sigBase64Decoded = sigChunkedXOR.let {
            val padding = when (it.length % 4) {
                2 -> "=="
                3 -> "="
                else -> ""
            }
            String(Base64.decode((it + padding).toByteArray(Charsets.UTF_8), Base64.DEFAULT))
        }
        val sigNew = sigBase64Decoded.dropLast(5).reversed().toCharArray().apply {
            for (i in indices step 2) {
                if (i + 1 < size) {
                    this[i] = this[i + 1].also { this[i + 1] = this[i] }
                }
            }
        }.concatToString().dropLast(5)
        return url.replace(sig, sigNew)
    }

    private interface MStreamDayExtractorService {
        companion object {
            fun build(baseUrl: String, originalLink: String): MStreamDayExtractorService {
                val retrofit = Retrofit.Builder().baseUrl(baseUrl)
                    .addConverterFactory(JsoupConverterFactory.create()).build()
                return retrofit.create(MStreamDayExtractorService::class.java)
            }
        }

        @GET
        suspend fun getSource(@Url url: String): Document
    }
}