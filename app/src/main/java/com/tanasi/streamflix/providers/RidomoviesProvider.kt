package com.tanasi.streamflix.providers

import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.extractors.Extractor
import com.tanasi.streamflix.models.Category
import com.tanasi.streamflix.models.Episode
import com.tanasi.streamflix.models.Genre
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.models.People
import com.tanasi.streamflix.models.Season
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.models.Video
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

object RidomoviesProvider : Provider {

    const val URL = "https://ridomovies.tv/"
    override val baseUrl = URL
    override val name = "Ridomovies"
    override val logo = "$URL/images/home-logo.png"
    override val language = "en"

    private val service = Service.build()

    override suspend fun getHome(): List<Category> {
        val document = service.getHome()

        val categories = mutableListOf<Category>()

        categories.add(
            Category(
                name = Category.FEATURED,
                list = document.select("div.carousel-container ul li").mapNotNull {
                    val id = it.selectFirst("a.btn-watch-now")
                        ?.attr("href")?.substringAfterLast("/")
                        ?: ""
                    val title = it.selectFirst("h3")
                        ?.text()
                        ?: ""
                    val overview = it.selectFirst("div.slider-item-plot")
                        ?.text()
                    val banner = it.selectFirst("img")
                        ?.attr("src")

                    val href = it.selectFirst("a.btn-watch-now")
                        ?.attr("href")
                        ?: ""
                    if (href.contains("movies/")) {
                        Movie(
                            id = id,
                            title = title,
                            overview = overview,
                            banner = banner,
                        )
                    } else if (href.contains("tv/")) {
                        TvShow(
                            id = id,
                            title = title,
                            overview = overview,
                            banner = banner,
                        )
                    } else {
                        null
                    }
                }
            )
        )

        categories.add(
            Category(
                name = "Latest Movies",
                list = document.select("section")
                    .find { it.selectFirst("div.section-title")?.text() == "Latest Movies" }
                    ?.select("div.poster")?.map {
                        Movie(
                            id = it.selectFirst("a")
                                ?.attr("href")?.substringAfterLast("/")
                                ?: "",
                            title = it.selectFirst("h3")
                                ?.text()
                                ?: "",
                            released = it.select("div.poster-details span").getOrNull(0)
                                ?.text(),
                            runtime = it.select("div.poster-details span").getOrNull(1)
                                ?.text()?.substringBefore(" min")?.toIntOrNull(),
                            quality = it.selectFirst("span.poster-type")
                                ?.text(),
                            poster = it.selectFirst("img")
                                ?.attr("src"),
                        )
                    }
                    ?: listOf()
            )
        )

        categories.add(
            Category(
                name = "Latest TV Series",
                list = document.select("section")
                    .find { it.selectFirst("div.section-title")?.text() == "Latest TV Series" }
                    ?.select("div.poster")?.map {
                        TvShow(
                            id = it.selectFirst("a")
                                ?.attr("href")?.substringAfterLast("/")
                                ?: "",
                            title = it.selectFirst("h3")
                                ?.text()
                                ?: "",
                            released = it.select("div.poster-details").getOrNull(0)
                                ?.text(),
                            runtime = it.select("div.poster-details").getOrNull(1)
                                ?.text()?.substringBefore(" min")?.toIntOrNull(),
                            quality = it.selectFirst("span.poster-type")
                                ?.text(),
                            poster = it.selectFirst("img")
                                ?.attr("src"),
                        )
                    }
                    ?: listOf()
            )
        )

        return categories
    }

    override suspend fun search(query: String, page: Int): List<AppAdapter.Item> {
        if (query.isEmpty()) {
            val response = service.getGenres()

            val genres = response.data.map {
                Genre(
                    id = it.slug,
                    name = it.name,
                )
            }

            return genres
        }

        val response = service.search(query, page)

        val results = response.data.items.mapNotNull {
            when (it.type) {
                "movie" -> Movie(
                    id = it.slug,
                    title = it.title,
                    overview = it.contentable.overview,
                    released = it.contentable.releaseDate,
                    runtime = it.contentable.duration.toInt(),
                    poster = it.contentable.apiPosterPath,
                    banner = it.contentable.apiBackdropPath,
                )

                "tv-series" -> TvShow(
                    id = it.slug,
                    title = it.title,
                    overview = it.contentable.overview,
                    released = it.contentable.releaseDate,
                    runtime = it.contentable.duration.toInt(),
                    poster = it.contentable.apiPosterPath,
                    banner = it.contentable.apiBackdropPath,
                )

                else -> null
            }
        }

        return results
    }

    override suspend fun getMovies(page: Int): List<Movie> {
        val response = service.getLatestMovies(page)

        val movies = response.data.items.map {
            Movie(
                id = it.content.slug,
                title = it.content.title,
                overview = it.overview,
                runtime = it.duration.toInt(),
                poster = it.posterPath.let { path ->
                    "${URL.trimEnd('/')}/${path.trimStart('/')}"},
            )
        }

        return movies
    }

    override suspend fun getTvShows(page: Int): List<TvShow> {
        val response = service.getLatestSeries(page)

        val tvShows = response.data.items.map {
            TvShow(
                id = it.content.slug,
                title = it.content.title,
                overview = it.overview,
                runtime = it.duration.toInt(),
                poster = it.posterPath.let { path ->
                    "${URL.trimEnd('/')}/${path.trimStart('/')}" },
            )
        }

        return tvShows
    }

    override suspend fun getMovie(id: String): Movie {
        val document = service.getMovie(id)

        val movie = Movie(
            id = id,
            title = document.selectFirst("h1")
                ?.text()
                ?: "",
            overview = document.selectFirst("div.text-overview-plot p")
                ?.text(),
            released = document.selectFirst("span.post-year")
                ?.text()?.substringBefore(")")?.substringAfter("("),
            runtime = document.select("div.info-cell")
                .find { it.selectFirst("strong")?.text() == "Duration: " }
                ?.ownText()?.let {
                    val hours = it.substringBefore("h").toIntOrNull() ?: 0
                    val minutes =
                        it.substringBeforeLast(" min").substringAfterLast(" ").toIntOrNull() ?: 0
                    if (hours * 60 + minutes != 0)
                        hours * 60 + minutes
                    else
                        null
                },
            rating = document.selectFirst("div.btn-imdb")
                ?.text()?.toDoubleOrNull(),
            poster = document.selectFirst("div.single-poster img")
                ?.attr("src"),

            genres = document.select("div.info-cell")
                .find { it.selectFirst("strong")?.text() == "Genre: " }
                ?.select("ul li")?.map {
                    Genre(
                        id = it.selectFirst("a")
                            ?.attr("href")?.substringAfterLast("/")
                            ?: "",
                        name = it.selectFirst("a")
                            ?.text()
                            ?: "",
                    )
                }
                ?: emptyList(),
            cast = document.select("div.cast-item").map {
                People(
                    id = "",
                    name = it.selectFirst("div.cast-name")
                        ?.text()
                        ?: "",
                    image = it.selectFirst("div.cast-image img")
                        ?.attr("src"),
                )
            },
        )

        return movie
    }

    override suspend fun getTvShow(id: String): TvShow {
        val document = service.getTv(id)

        val tvShow = TvShow(
            id = id,
            title = document.selectFirst("h1")
                ?.text()
                ?: "",
            overview = document.selectFirst("div.text-overview-plot p")
                ?.text(),
            released = document.selectFirst("span.post-year")
                ?.text()?.substringBefore(")")?.substringAfter("("),
            runtime = document.select("div.info-cell")
                .find { it.selectFirst("strong")?.text() == "Duration: " }
                ?.ownText()?.let {
                    val hours = it.substringBefore("h").toIntOrNull() ?: 0
                    val minutes =
                        it.substringBeforeLast(" min").substringAfterLast(" ").toIntOrNull() ?: 0
                    if (hours * 60 + minutes != 0)
                        hours * 60 + minutes
                    else
                        null
                },
            rating = document.selectFirst("div.btn-imdb")
                ?.text()?.toDoubleOrNull(),
            poster = document.selectFirst("div.single-poster img")
                ?.attr("src"),

            seasons = service.getSeasons(id).data.items.map {
                Season(
                    id = "$id/${it.id}",
                    number = it.seasonNumber.toInt(),
                    title = "Season ${it.seasonNumber}",
                )
            },
            genres = document.select("div.info-cell")
                .find { it.selectFirst("strong")?.text() == "Genre: " }
                ?.select("ul li")?.map {
                    Genre(
                        id = it.selectFirst("a")
                            ?.attr("href")?.substringAfterLast("/")
                            ?: "",
                        name = it.selectFirst("a")
                            ?.text()
                            ?: "",
                    )
                }
                ?: emptyList(),
            cast = document.select("div.cast-item").map {
                People(
                    id = "",
                    name = it.selectFirst("div.cast-name")
                        ?.text()
                        ?: "",
                    image = it.selectFirst("div.cast-image img")
                        ?.attr("src"),
                )
            },
        )

        return tvShow
    }

    override suspend fun getEpisodesBySeason(seasonId: String): List<Episode> {
        val (tvShowId, id) = seasonId.split("/")

        val response = service.getEpisodes(tvShowId, id)

        val episodes = response.data.items.map {
            Episode(
                id = it.id,
                number = it.episodeNumber,
                title = it.title,
                released = it.releaseDate,
            )
        }

        return episodes
    }

    override suspend fun getGenre(id: String, page: Int): Genre {
        val response = service.getGenre(id, page)

        val genre = Genre(
            id = id,
            name = "",
            shows = response.data.items.mapNotNull {
                when (it.content.type) {
                    "movie" -> Movie(
                        id = it.content.slug,
                        title = it.content.title,
                        overview = it.overview,
                        released = it.releaseYear,
                        runtime = it.duration.toInt(),
                        poster = it.posterPath.let { path -> "$URL/$path" },
                    )

                    "tv-series" -> TvShow(
                        id = it.content.slug,
                        title = it.content.title,
                        overview = it.overview,
                        released = it.releaseYear,
                        runtime = it.duration.toInt(),
                        poster = it.posterPath.let { path -> "$URL/$path" },
                    )

                    else -> null
                }
            }
        )

        return genre
    }

    override suspend fun getPeople(id: String, page: Int): People {
        throw Exception("Not yet implemented")
    }

    override suspend fun getServers(id: String, videoType: Video.Type): List<Video.Server> {
        val response = when (videoType) {
            is Video.Type.Episode -> service.getEpisodeVideos(id)
            is Video.Type.Movie -> service.getMovieVideos(id)
        }

        val servers = response.data.mapNotNull {
            Video.Server(
                id = it.id,
                name = it.quality,
                src = Jsoup.parse(it.url).selectFirst("iframe")
                    ?.attr("data-src")
                    ?: return@mapNotNull null,
            )
        }

        return servers
    }

    override suspend fun getVideo(server: Video.Server): Video {
        return Extractor.extract(server.src)
    }


    private interface Service {

        companion object {
            private const val DNS_QUERY_URL = "https://1.1.1.1/dns-query"

            fun build(): Service {
                val dohClient = OkHttpClient()

                val client = OkHttpClient.Builder()
                    .readTimeout(30, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .dns(
                        DnsOverHttps.Builder()
                            .client(dohClient)
                            .url(DNS_QUERY_URL.toHttpUrl())
                            .build()
                    )
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                            .addHeader("Accept-Language", "en-US,en;q=0.5")
                            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
                            .addHeader("Platform", "android")
                            .build()
                        chain.proceed(request)
                    }
                    .build()

                val retrofit = Retrofit.Builder()
                    .baseUrl(URL)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build()

                return retrofit.create(Service::class.java)
            }
        }



        @GET("home")
        suspend fun getHome(): Document

        @GET("core/api/movies/latest")
        suspend fun getLatestMovies(
            @Query("page[number]") page: Int = 1,
        ): Response<DataItems<Show>>

        @GET("core/api/series/latest")
        suspend fun getLatestSeries(
            @Query("page[number]") page: Int = 1,
        ): Response<DataItems<Show>>

        @GET("core/api/search")
        suspend fun search(
            @Query("q") q: String,
            @Query("page[number]") page: Int = 1,
        ): Response<DataItems<SearchItem>>

        @GET("movies/{slug}")
        suspend fun getMovie(
            @Path("slug") slug: String,
        ): Document

        @GET("tv/{slug}")
        suspend fun getTv(
            @Path("slug") slug: String,
        ): Document

        @GET("core/api/series/{slug}/seasons")
        suspend fun getSeasons(
            @Path("slug") slug: String,
        ): Response<DataItems<Seasons>>

        @GET("core/api/series/{slug}/seasons/{seasonId}/episodes")
        suspend fun getEpisodes(
            @Path("slug") slug: String,
            @Path("seasonId") seasonId: String,
        ): Response<DataItems<Episode>>

        @GET("core/api/genres")
        suspend fun getGenres(): Response<List<Genre>>

        @GET("core/api/genres/{genre}/contents")
        suspend fun getGenre(
            @Path("genre") genre: String,
            @Query("page[number]") page: Int = 1,
        ): Response<DataItems<Show>>

        @GET("core/api/movies/{slug}/videos")
        suspend fun getMovieVideos(
            @Path("slug") slug: String,
        ): Response<List<Video>>

        @GET("core/api/episodes/{id}/videos")
        suspend fun getEpisodeVideos(
            @Path("id") id: String,
        ): Response<List<Video>>


        data class Response<T>(
            val code: Int,
            val message: String,
            val data: T,
        )

        data class DataItems<T>(
            val items: List<T>,
            val pagination: Pagination,
        ) {
            data class Pagination(
                val hasNext: Boolean,
                val hasPrev: Boolean,
                val pageNumber: Int,
                val pageSize: Int,
                val totalPages: Int,
                val totalRecords: Int,
            )
        }

        data class Show(
            val id: String,
            val contentId: String,
            val overview: String,
            val releaseYear: String,
            val imdbRating: Double,
            val imdbId: String,
            val duration: Long,
            val countryCode: String,
            val posterNote: Any?,
            val posterPath: String,
            val mpaaRating: Any?,
            val content: Content,
            val country: Country,
        ) {
            data class Content(
                val id: String,
                val type: String,
                val slug: String,
                val title: String,
                val fullSlug: String,
                val genres: List<Genre>,
            ) {
                data class Genre(
                    val id: Long,
                    val name: String,
                    val slug: String,
                    val fullSlug: String,
                )
            }

            data class Country(
                val name: String,
                val slug: String,
                val code: String,
                val fullSlug: String,
            )
        }

        data class SearchItem(
            val id: String,
            val type: String,
            val slug: String,
            val title: String,
            val metaTitle: Any?,
            val metaDescription: Any?,
            val usersOnly: Boolean,
            val userLevel: Long,
            val vipOnly: Boolean,
            val copyrighted: Boolean,
            val status: String,
            val publishedAt: String,
            val createdAt: String,
            val updatedAt: String,
            val fullSlug: String,
            val contentable: Contentable,
        ) {
            data class Contentable(
                val id: String,
                val contentId: String,
                val revisionId: Any?,
                val originalTitle: String,
                val overview: String,
                val releaseDate: String,
                val releaseYear: String,
                val videoNote: Any?,
                val posterNote: Any?,
                val userRating: Double,
                val imdbRating: Double,
                val imdbVotes: Long,
                val imdbId: String,
                val duration: Long,
                val countryCode: String,
                val posterPath: String,
                val backdropPath: String,
                val apiPosterPath: String,
                val apiBackdropPath: String,
                val trailerUrl: String,
                val mpaaRating: String,
                val tmdbId: Long,
                val manual: Long,
                val directorId: Long,
                val createdAt: String,
                val updatedAt: String,
                val content: Content,
            ) {
                data class Content(
                    val id: String,
                    val type: String,
                    val slug: String,
                    val title: String,
                    val metaTitle: Any?,
                    val metaDescription: Any?,
                    val usersOnly: Boolean,
                    val userLevel: Long,
                    val vipOnly: Boolean,
                    val copyrighted: Boolean,
                    val status: String,
                    val publishedAt: String,
                    val createdAt: String,
                    val updatedAt: String,
                    val fullSlug: String,
                )
            }
        }

        data class Seasons(
            val id: Long,
            val tvId: String,
            val seasonNumber: Long,
            val createdAt: String,
            val updatedAt: String,
            val episodes: List<Episode>,
        )

        data class Episode(
            val id: String,
            val slug: String,
            val seasonId: Long,
            val episodeNumber: Int,
            val title: String,
            val overview: String?,
            val releaseDate: String,
            val releaseYear: String,
            val videoNote: Any?,
            val posterNote: Any?,
            val duration: Long,
            val tmdbId: String,
            val trailerUrl: Any?,
            val publishedAt: String,
            val status: String,
            val createdAt: String,
            val updatedAt: String,
            val fullSlug: String,
        )

        data class Genre(
            val id: Long,
            val name: String,
            val slug: String,
            val apiSlug: String,
            val description: String?,
            val metaTitle: Any?,
            val metaDescription: Any?,
            val publishedAt: String?,
            val status: String,
            val createdAt: String,
            val updatedAt: String,
            val fullSlug: String,
        )

        data class Video(
            val id: String,
            val link: String,
            val lang: String,
            val quality: String,
            val publishedAt: Any?,
            val status: String,
            val url: String,
        )
    }
}