package com.tanasi.streamflix.providers

import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.fragments.player.PlayerFragment
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
import java.util.concurrent.TimeUnit

object AniwatchProvider : Provider {

    override val name = "Aniwatch"
    override val logo = "https://aniwatch.to/images/logo.png"
    override val url = "https://aniwatch.to/"

    private val service = AniwatchService.build()


    override suspend fun getHome(): List<Category> {
        TODO("Not yet implemented")
    }

    override suspend fun search(query: String): List<AppAdapter.Item> {
        TODO("Not yet implemented")
    }

    override suspend fun getMovies(): List<Movie> {
        TODO("Not yet implemented")
    }

    override suspend fun getTvShows(): List<TvShow> {
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


    override suspend fun getGenre(id: String): Genre {
        TODO("Not yet implemented")
    }


    override suspend fun getPeople(id: String): People {
        TODO("Not yet implemented")
    }


    override suspend fun getVideo(id: String, videoType: PlayerFragment.VideoType): Video {
        TODO("Not yet implemented")
    }


    interface AniwatchService {

        companion object {
            fun build(): AniwatchService {
                val client = OkHttpClient.Builder()
                    .readTimeout(30, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .build()

                val retrofit = Retrofit.Builder()
                    .baseUrl(url)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build()

                return retrofit.create(AniwatchService::class.java)
            }
        }
    }
}