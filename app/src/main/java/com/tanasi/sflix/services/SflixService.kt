package com.tanasi.sflix.services

import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface SflixService {

    companion object {
        fun build(): SflixService {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://sflix.to/")
                .addConverterFactory(JsoupConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(SflixService::class.java)
        }
    }

    @GET("home")
    suspend fun fetchHome(): Document


    @GET("movie/free-{id}")
    suspend fun fetchMovie(@Path("id") id: String): Document

    @GET("ajax/movie/episodes/{id}")
    suspend fun fetchMovieServers(@Path("id") movieId: String): Document


    @GET("ajax/get_link/{id}")
    suspend fun getLink(@Path("id") id: String): Link

    @GET
    @Headers(
        "accept: */*",
        "referer: https://sflix.to",
        "x-requested-with: XMLHttpRequest",
    )
    suspend fun getSources(
        @Url url: String,
        @Query("id") id: String,
    ): Sources


    data class Link(
        val type: String = "",
        val link: String = "",
        val sources: List<String> = listOf(),
        val tracks: List<String> = listOf(),
        val title: String = "",
    )

    data class Sources(
        val sources: List<Source> = listOf(),
        val sourcesBackup: List<Source> = listOf(),
        val tracks: List<Track> = listOf(),
        val server: Int? = null,
    ) {
        data class Source(
            val file: String = "",
            val type: String = "",
        )

        data class Track(
            val file: String = "",
            val label: String = "",
            val kind: String = "",
            val default: Boolean = false,
        )
    }
}
