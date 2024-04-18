package com.tanasi.streamflix.utils

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

object OpenSubtitles {

    private const val URL = "https://rest.opensubtitles.org/"

    private val service = Service.build()

    suspend fun search(
        imdbId: Int,
        languageId: String,
    ): List<Subtitle> {
        return try {
            service.search(imdbId, languageId)
        } catch (_: Exception) {
            emptyList()
        }
    }


    private interface Service {

        companion object {
            fun build(): Service {
                val retrofit = Retrofit.Builder()
                    .baseUrl(URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                return retrofit.create(Service::class.java)
            }
        }


        @GET("search/imdbid-{imdbId}/sublanguageid-{languageId}")
        suspend fun search(
            @Path("imdbId") imdbId: Int,
            @Path("languageId") languageId: String,
        ): List<Subtitle>
    }

    data class Subtitle(
        @SerializedName("MatchedBy") val matchedBy: String? = null,
        @SerializedName("IDSubMovieFile") val idSubMovieFile: String? = null,
        @SerializedName("MovieHash") val movieHash: String? = null,
        @SerializedName("MovieByteSize") val movieByteSize: String? = null,
        @SerializedName("MovieTimeMS") val movieTimeMS: String? = null,
        @SerializedName("IDSubtitleFile") val idSubtitleFile: String? = null,
        @SerializedName("SubFileName") val subFileName: String? = null,
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
        @SerializedName("SubDownloadLink") val subDownloadLink: String? = null,
        @SerializedName("ZipDownloadLink") val zipDownloadLink: String? = null,
        @SerializedName("SubtitlesLink") val subtitlesLink: String? = null,
        @SerializedName("QueryNumber") val queryNumber: String? = null,
        @SerializedName("Score") val score: Double? = null
    )
}