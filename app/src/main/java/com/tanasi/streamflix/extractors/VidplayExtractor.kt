package com.tanasi.streamflix.extractors

import android.util.Base64
import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.models.Video
import com.tanasi.streamflix.utils.StringConverterFactory
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Url
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

open class VidplayExtractor : Extractor() {

    override val name = "Vidplay"
    override val mainUrl = "https://vidplay.site"
    open val key = "https://raw.githubusercontent.com/KillerDogeEmpire/vidplay-keys/keys/keys.json"

    override suspend fun extract(link: String): Video {
        val service = Service.build(mainUrl)

        val id = link.substringBefore("?").substringAfterLast("/")
        val encodeId = encodeId(id, service.getKeys(key))
        val mediaUrl = callFutoken(encodeId, link, service.getFutoken(link))
            ?: throw Exception("Media URL null")
        val response = service.getSources(mediaUrl, link)

        val video = Video(
            source = response.result?.sources?.first()?.file
                ?: throw Exception("Can't retrieve source"),
            subtitles = response.result.tracks
                ?.filter { it.kind == "captions" }
                ?.mapNotNull {
                    Video.Subtitle(
                        it.label ?: "Unknow",
                        it.file ?: return@mapNotNull null,
                    )
                }
                ?: listOf()
        )

        return video
    }

    private fun callFutoken(id: String, url: String, script: String): String? {
        val k = "k='(\\S+)'".toRegex().find(script)?.groupValues?.get(1) ?: return null
        val a = mutableListOf(k)
        for (i in id.indices) {
            a.add((k[i % k.length].code + id[i].code).toString())
        }
        return "$mainUrl/mediainfo/${a.joinToString(",")}?${url.substringAfter("?")}"
    }

    private fun encodeId(id: String, keyList: List<String>): String {
        val cipher1 = Cipher.getInstance("RC4")
        val cipher2 = Cipher.getInstance("RC4")
        cipher1.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(keyList[0].toByteArray(), "RC4"),
            cipher1.parameters
        )
        cipher2.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(keyList[1].toByteArray(), "RC4"),
            cipher2.parameters
        )
        var input = id.toByteArray()
        input = cipher1.doFinal(input)
        input = cipher2.doFinal(input)
        return Base64.encode(input, Base64.NO_WRAP).toString(Charsets.UTF_8).replace("/", "_")
    }

    class Any(hostUrl: String) : VidplayExtractor() {
        override val mainUrl = hostUrl
    }

    class MyCloud : VidplayExtractor() {
        override val name = "MyCloud"
        override val mainUrl = "https://mcloud.bz"
    }

    class VidplayOnline : VidplayExtractor() {
        override val mainUrl = "https://vidplay.online"
    }


    private interface Service {

        companion object {
            fun build(baseUrl: String): Service {
                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .addConverterFactory(StringConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                return retrofit.create(Service::class.java)
            }
        }

        @GET
        @Headers(
            "Accept: application/json, text/javascript, */*; q=0.01",
            "X-Requested-With: XMLHttpRequest",
        )
        suspend fun getSources(
            @Url url: String,
            @Header("referer") referer: String,
        ): Response

        @GET
        suspend fun getKeys(@Url url: String): List<String>

        @GET("futoken")
        suspend fun getFutoken(@Header("referer") referer: String): String
    }


    data class Response(
        val result: Result? = null,
    ) {

        data class Result(
            val sources: List<Sources>? = listOf(),
            val tracks: List<Tracks>? = listOf(),
        ) {

            data class Tracks(
                val file: String? = null,
                val label: String? = null,
                val kind: String? = null,
            )

            data class Sources(
                val file: String? = null,
            )
        }
    }

}