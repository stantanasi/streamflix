package com.tanasi.streamflix.providers

import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
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

object UnJourUnFilmProvider : Provider {

    override val name = "1JOUR1FILM"
    override val logo =
        "https://1jour1film.pics/wp-content/uploads/2022/12/screenshot-looka.com-2021.10.19-12_28_21.png"
    override val language = "fr"
    private const val URL = "https://1jour1film.pics/"

    private val service = Service.build()

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


    private interface Service {

        companion object {
            fun build(): Service {
                val client = OkHttpClient.Builder()
                    .build()

                val retrofit = Retrofit.Builder()
                    .baseUrl(URL)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .client(client)
                    .build()

                return retrofit.create(Service::class.java)
            }
        }
    }
}