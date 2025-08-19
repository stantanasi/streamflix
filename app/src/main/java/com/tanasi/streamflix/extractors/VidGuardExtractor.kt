package com.tanasi.streamflix.extractors

import android.util.Base64
import com.tanasi.streamflix.models.Video
import com.tanasi.streamflix.utils.JsUnpacker
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url

class VidGuardExtractor : Extractor() {
    override val name = "VidGuard"
    override val mainUrl = "https://vidguard.to"
    override val aliasUrls = listOf(
        "vembed.net", "bembed.cc", "vgfplay.com", "listeamed.net", "vidguard.to"
    )

    private val client = OkHttpClient()
    private val service = Retrofit.Builder()
        .baseUrl(mainUrl)
        .addConverterFactory(ScalarsConverterFactory.create())
        .client(client)
        .build()
        .create(VidGuardService::class.java)

    private interface VidGuardService {
        @GET
        suspend fun get(@Url url: String): String
    }

    override suspend fun extract(link: String): Video {
        val pageHtml = try {
            service.get(link)
        } catch (e: Exception) {
            // A veces la URL viene sin el protocolo https
            service.get("https:$link")
        }

        val scriptData = pageHtml
            .substringAfter("eval(function(p,a,c,k,e,d)")
            .substringBefore("</script>")
            .let { "eval(function(p,a,c,k,e,d)$it" }

        if (!scriptData.startsWith("eval")) {
            throw Exception("No se encontró el script eval. El HTML puede haber cambiado.")
        }

        val unpackedScript = JsUnpacker(scriptData).unpack()
            ?: throw Exception("No se pudo desempacar el script..")

        val urlEncoded = unpackedScript
            .substringAfter("window.svg={\"stream\":\"")
            .substringBefore("\",\"hash")

        val finalUrl = sigDecode(urlEncoded)

        return Video(
            source = finalUrl,
            headers = mapOf("Referer" to mainUrl)
        )
    }

    private fun sigDecode(url: String): String {
        val sig = url.split("sig=")[1].split("&")[0]
        val decodedSig = sig.chunked(2)
            .joinToString("") { (Integer.parseInt(it, 16) xor 2).toChar().toString() }
            .let {
                val padding = when (it.length % 4) {
                    2 -> "=="
                    3 -> "="
                    else -> ""
                }
                String(Base64.decode((it + padding).toByteArray(), Base64.DEFAULT))
            }
            .dropLast(5)
            .reversed()
            .toCharArray()
            .apply {
                for (i in indices step 2) {
                    if (i + 1 < size) {
                        this[i] = this[i + 1].also { this[i + 1] = this[i] }
                    }
                }
            }
            .concatToString()
            .dropLast(5)
        return url.replace(sig, decodedSig)
    }
}