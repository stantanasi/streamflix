package com.tanasi.sflix.services

import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path

interface SflixService {

    companion object {
        fun build(): SflixService {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://sflix.to/")
                .addConverterFactory(JsoupConverterFactory.create())
                .build()

            return retrofit.create(SflixService::class.java)
        }
    }

    @GET("home")
    suspend fun fetchHome(): Document

    @GET("movie/free-{id}")
    suspend fun fetchMovie(@Path("id") id: String): Document
}
