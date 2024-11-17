package com.tanasi.streamflix.extractors

import android.util.Base64
import androidx.media3.common.MimeTypes
import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.models.Video
import com.tanasi.streamflix.providers.RidomoviesProvider
import com.tanasi.streamflix.utils.JsUnpacker
import okhttp3.OkHttpClient
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url

class CloseloadExtractor : Extractor() {

    override val name = "Closeload"
    override val mainUrl = "https://closeload.top/"

    override suspend fun extract(link: String): Video {
        val service = Service.build(mainUrl)

        val document = service.get(link, RidomoviesProvider.URL)

        val unpacked = JsUnpacker(document.toString())
            .unpack()
            ?: throw Exception("Can't unpack JS")

        val source = Regex("=\"(aHR.*?)\";").find(unpacked)
            ?.groupValues?.get(1)
            ?.let { Base64.decode(it, Base64.DEFAULT).toString(Charsets.UTF_8) }
            ?: throw Exception("Can't retrieve source")

        return Video(
            source = source,
            headers = mapOf(
                "Referer" to mainUrl,
            ),
            type = MimeTypes.APPLICATION_M3U8,
        )
    }

    private interface Service {

        companion object {
            fun build(baseUrl: String): Service {
                val client = OkHttpClient.Builder()
                    .build()

                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build()

                return retrofit.create(Service::class.java)
            }
        }

        @GET
        suspend fun get(
            @Url url: String,
            @Header("referer") referer: String,
        ): Document
    }
}