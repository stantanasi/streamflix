package com.tanasi.streamflix.utils

import android.net.Uri
import androidx.core.net.toUri
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URL
import java.util.zip.GZIPInputStream

object OpenSubtitles {

    private const val URL = "https://rest.opensubtitles.org/"

    private val service = Service.build()

    suspend fun download(
        subtitle: Subtitle,
    ): Uri = withContext(Dispatchers.IO) {
        val zip = File.createTempFile(
            "${File(subtitle.subFileName).nameWithoutExtension}-",
            ".${File(subtitle.subDownloadLink).extension}"
        )

        URL(subtitle.subDownloadLink).openStream().use { input ->
            FileOutputStream(zip).use { output -> input.copyTo(output) }
        }

        val subtitleFile = File("${zip.parent}${File.separator}${subtitle.subFileName}")

        if (subtitleFile.exists()) {
            subtitleFile.delete()
        }

        FileInputStream(zip).use { fileInputStream ->
            GZIPInputStream(fileInputStream).use { gzipInputStream ->
                FileOutputStream(subtitleFile).use { fileOutputStream ->
                    val buffer = ByteArray(1024)
                    var bytesRead: Int

                    while (gzipInputStream.read(buffer).also { bytesRead = it } != -1) {
                        fileOutputStream.write(buffer, 0, bytesRead)
                    }
                }
            }
        }

        zip.delete()

        subtitleFile.toUri()
    }

    suspend fun search(
        imdbId: String? = null,
        query: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subLanguageId: String? = null,
    ): List<Subtitle> {
        val params = mapOf(
            Params.Key.IMDB_ID to imdbId,
            Params.Key.QUERY to query,
            Params.Key.SEASON to season?.toString(),
            Params.Key.EPISODE to episode?.toString(),
            Params.Key.SUB_LANGUAGE_ID to subLanguageId,
        )
        return service.search(
            params = params
                .filterNotNullValues()
                .map { "${it.key}-${it.value}" }
                .joinToString("/")
        )
    }

    object Params {

        object Key {
            const val IMDB_ID = "imdbid"
            const val QUERY = "query"
            const val EPISODE = "episode"
            const val SEASON = "season"
            const val SUB_LANGUAGE_ID = "sublanguageid"
        }
    }


    private interface Service {

        companion object {
            fun build(): Service {
                val client = OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val requestBuilder = chain.request().newBuilder()
                            .addHeader("User-Agent", "TemporaryUserAgent")

                        chain.proceed(requestBuilder.build())
                    }
                    .build()

                val retrofit = Retrofit.Builder()
                    .baseUrl(URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                return retrofit.create(Service::class.java)
            }
        }


        @GET("search/{params}")
        suspend fun search(
            @Path("params", encoded = true) params: String,
        ): List<Subtitle>
    }

    data class Subtitle(
        @SerializedName("MatchedBy") val matchedBy: String? = null,
        @SerializedName("IDSubMovieFile") val idSubMovieFile: String? = null,
        @SerializedName("MovieHash") val movieHash: String? = null,
        @SerializedName("MovieByteSize") val movieByteSize: String? = null,
        @SerializedName("MovieTimeMS") val movieTimeMS: String? = null,
        @SerializedName("IDSubtitleFile") val idSubtitleFile: String? = null,
        @SerializedName("SubFileName") val subFileName: String = "",
        @SerializedName("SubActualCD") val subActualCD: String? = null,
        @SerializedName("SubSize") val subSize: String? = null,
        @SerializedName("SubHash") val subHash: String? = null,
        @SerializedName("SubLastTS") val subLastTS: String? = null,
        @SerializedName("SubTSGroup") val subTSGroup: String? = null,
        @SerializedName("InfoReleaseGroup") val infoReleaseGroup: String? = null,
        @SerializedName("InfoFormat") val infoFormat: String? = null,
        @SerializedName("InfoOther") val infoOther: String? = null,
        @SerializedName("IDSubtitle") val idSubtitle: String? = null,
        @SerializedName("UserID") val userID: String? = null,
        @SerializedName("SubLanguageID") val subLanguageID: String? = null,
        @SerializedName("SubFormat") val subFormat: String? = null,
        @SerializedName("SubSumCD") val subSumCD: String? = null,
        @SerializedName("SubAuthorComment") val subAuthorComment: String? = null,
        @SerializedName("SubAddDate") val subAddDate: String? = null,
        @SerializedName("SubBad") val subBad: String? = null,
        @SerializedName("SubRating") val subRating: String? = null,
        @SerializedName("SubSumVotes") val subSumVotes: String? = null,
        @SerializedName("SubDownloadsCnt") val subDownloadsCnt: String? = null,
        @SerializedName("MovieReleaseName") val movieReleaseName: String? = null,
        @SerializedName("MovieFPS") val movieFPS: String? = null,
        @SerializedName("IDMovie") val idMovie: String? = null,
        @SerializedName("IDMovieImdb") val idMovieImdb: String? = null,
        @SerializedName("MovieName") val movieName: String? = null,
        @SerializedName("MovieNameEng") val movieNameEng: String? = null,
        @SerializedName("MovieYear") val movieYear: String? = null,
        @SerializedName("MovieImdbRating") val movieImdbRating: String? = null,
        @SerializedName("SubFeatured") val subFeatured: String? = null,
        @SerializedName("UserNickName") val userNickName: String? = null,
        @SerializedName("SubTranslator") val subTranslator: String? = null,
        @SerializedName("ISO639") val iso639: String? = null,
        @SerializedName("LanguageName") val languageName: String? = null,
        @SerializedName("SubComments") val subComments: String? = null,
        @SerializedName("SubHearingImpaired") val subHearingImpaired: String? = null,
        @SerializedName("UserRank") val userRank: String? = null,
        @SerializedName("SeriesSeason") val seriesSeason: String? = null,
        @SerializedName("SeriesEpisode") val seriesEpisode: String? = null,
        @SerializedName("MovieKind") val movieKind: String? = null,
        @SerializedName("SubHD") val subHD: String? = null,
        @SerializedName("SeriesIMDBParent") val seriesIMDBParent: String? = null,
        @SerializedName("SubEncoding") val subEncoding: String? = null,
        @SerializedName("SubAutoTranslation") val subAutoTranslation: String? = null,
        @SerializedName("SubForeignPartsOnly") val subForeignPartsOnly: String? = null,
        @SerializedName("SubFromTrusted") val subFromTrusted: String? = null,
        @SerializedName("QueryCached") val queryCached: Int? = null,
        @SerializedName("SubDownloadLink") val subDownloadLink: String = "",
        @SerializedName("ZipDownloadLink") val zipDownloadLink: String? = null,
        @SerializedName("SubtitlesLink") val subtitlesLink: String? = null,
        @SerializedName("QueryNumber") val queryNumber: String? = null,
        @SerializedName("Score") val score: Double? = null
    )
}