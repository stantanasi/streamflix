package com.tanasi.sflix.providers

import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.sflix.fragments.player.PlayerFragment
import com.tanasi.sflix.models.*
import retrofit2.Retrofit

object AllMoviesForYouProvider : Provider {

    private val service = AllMoviesForYouService.build()


    override suspend fun getHome(): List<Category> {
        TODO("Not yet implemented")
    }

    override suspend fun search(query: String): List<Show> {
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

    override suspend fun getSeasonEpisodes(seasonId: String): List<Episode> {
        TODO("Not yet implemented")
    }


    override suspend fun getPeople(id: String): People {
        TODO("Not yet implemented")
    }


    override suspend fun getVideo(id: String, videoType: PlayerFragment.VideoType): Video {
        TODO("Not yet implemented")
    }


    interface AllMoviesForYouService {

        companion object {
            fun build(): AllMoviesForYouService {
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://allmoviesforyou.net/")
                    .addConverterFactory(JsoupConverterFactory.create())
                    .build()

                return retrofit.create(AllMoviesForYouService::class.java)
            }
        }
    }
}