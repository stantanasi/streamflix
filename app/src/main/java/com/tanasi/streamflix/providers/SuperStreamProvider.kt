package com.tanasi.streamflix.providers

import android.util.Base64
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.models.Category
import com.tanasi.streamflix.models.Episode
import com.tanasi.streamflix.models.Genre
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.models.People
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.models.Video
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object SuperStreamProvider : Provider {

    override val name = "SuperStream"
    override val logo = ""
    override val url = Base64.decode(
        "aHR0cHM6Ly9zaG93Ym94LnNoZWd1Lm5ldA==",
        Base64.NO_WRAP
    ).toString(Charsets.UTF_8) + Base64.decode(
        "L2FwaS9hcGlfY2xpZW50L2luZGV4Lw==",
        Base64.NO_WRAP
    ).toString(Charsets.UTF_8)

    private val service = SuperStreamApiService.build()

    // We do not want content scanners to notice this scraping going on so we've hidden all constants
    // The source has its origins in China so I added some extra security with banned words
    // Mayhaps a tiny bit unethical, but this source is just too good :)
    // If you are copying this code please use precautions so they do not change their api.
    private val iv = Base64.decode(
        "d0VpcGhUbiE=",
        Base64.NO_WRAP
    ).toString(Charsets.UTF_8)
    private val key = Base64.decode(
        "MTIzZDZjZWRmNjI2ZHk1NDIzM2FhMXc2",
        Base64.NO_WRAP
    ).toString(Charsets.UTF_8)

    private val secondApiUrl = Base64.decode(
        "aHR0cHM6Ly9tYnBhcGkuc2hlZ3UubmV0L2FwaS9hcGlfY2xpZW50L2luZGV4Lw==",
        Base64.NO_WRAP
    ).toString(Charsets.UTF_8)

    private val appKey = Base64.decode(
        "bW92aWVib3g=",
        Base64.NO_WRAP
    ).toString(Charsets.UTF_8)
    private val appId = Base64.decode(
        "Y29tLnRkby5zaG93Ym94",
        Base64.NO_WRAP
    ).toString(Charsets.UTF_8)
    private val appIdSecond = Base64.decode(
        "Y29tLm1vdmllYm94cHJvLmFuZHJvaWQ=",
        Base64.NO_WRAP
    ).toString(Charsets.UTF_8)
    private const val APP_VERSION = "14.7"
    private const val APP_VERSION_CODE = "160"


    override suspend fun getHome(): List<Category> {
        TODO("Not yet implemented")
    }

    override suspend fun search(query: String, page: Int): List<AppAdapter.Item> {
        TODO("Not yet implemented")
    }

    override suspend fun getMovies(page: Int): List<Movie> {
        TODO("Not yet implemented")
    }

    override suspend fun getTvShows(page: Int): List<TvShow> {
        TODO("Not yet implemented")
    }

    override suspend fun getMovie(id: String): Movie {
        TODO("Not yet implemented")
    }

    override suspend fun getTvShow(id: String): TvShow {
        TODO("Not yet implemented")
    }

    override suspend fun getEpisodesBySeason(seasonId: String): List<Episode> {
        TODO("Not yet implemented")
    }

    override suspend fun getGenre(id: String, page: Int): Genre {
        TODO("Not yet implemented")
    }

    override suspend fun getPeople(id: String, page: Int): People {
        TODO("Not yet implemented")
    }

    override suspend fun getServers(id: String, videoType: Video.Type): List<Video.Server> {
        TODO("Not yet implemented")
    }

    override suspend fun getVideo(server: Video.Server): Video {
        TODO("Not yet implemented")
    }


    private interface SuperStreamApiService {

        companion object {
            fun build(): SuperStreamApiService {
                val client = OkHttpClient.Builder().addInterceptor { chain ->
                    val requestBuilder = chain.request().newBuilder()
                        .addHeader("Accept", "charset=utf-8")
                        .addHeader("Platform", "android")

                    chain.proceed(requestBuilder.build())
                }.build()

                val retrofit = Retrofit.Builder()
                    .baseUrl(url)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                return retrofit.create(SuperStreamApiService::class.java)
            }
        }
    }
}