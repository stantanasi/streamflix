package com.tanasi.streamflix.providers

import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.BuildConfig
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
import okhttp3.OkHttpClient
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

object SflixProvider : Provider {

    private const val URL = "https://sflix.to/"
    override val baseUrl = URL
    override val name = "SFlix"
    override val logo = "https://img.sflix.to/xxrz/400x400/100/66/35/66356c25ce98cb12993249e21742b129/66356c25ce98cb12993249e21742b129.png"
    override val language = "en"

    private val service = SflixService.build()


    override suspend fun getHome(): List<Category> {
        val document = service.getHome()

        val categories = mutableListOf<Category>()

        categories.add(
            Category(
                name = Category.FEATURED,
                list = document.select("div.swiper-wrapper > div.swiper-slide").map {
                    val id = it.selectFirst("a")
                        ?.attr("href")?.substringAfterLast("-") ?: ""
                    val title = it.selectFirst("h2.film-title")
                        ?.text() ?: ""
                    val overview = it.selectFirst("p.sc-desc")
                        ?.text()
                    val info = it.select("div.sc-detail > div.scd-item").toInfo()
                    val poster = it.selectFirst("img.film-poster-img")
                        ?.attr("src")
                    val banner = it.selectFirst("div.slide-photo img")
                        ?.attr("src")

                    if (it.isMovie()) {
                        Movie(
                            id = id,
                            title = title,
                            overview = overview,
                            released = info.released,
                            quality = info.quality,
                            rating = info.rating,
                            poster = poster,
                            banner = banner,
                        )
                    } else {
                        TvShow(
                            id = id,
                            title = title,
                            overview = overview,
                            quality = info.quality,
                            rating = info.rating,
                            poster = poster,
                            banner = banner,

                            seasons = info.lastEpisode?.let { lastEpisode ->
                                listOf(
                                    Season(
                                        id = "",
                                        number = lastEpisode.season,

                                        episodes = listOf(
                                            Episode(
                                                id = "",
                                                number = lastEpisode.episode,
                                            )
                                        )
                                    )
                                )
                            } ?: listOf(),
                        )
                    }
                },
            )
        )

        categories.add(
            Category(
                name = "Trending Movies",
                list = document.select("div#trending-movies div.flw-item").map {
                    val info = it.select("div.film-detail > div.fd-infor > span").toInfo()

                    Movie(
                        id = it.selectFirst("a")
                            ?.attr("href")?.substringAfterLast("-") ?: "",
                        title = it.selectFirst("h3.film-name")
                            ?.text() ?: "",
                        released = info.released,
                        quality = info.quality,
                        rating = info.rating,
                        poster = it.selectFirst("div.film-poster > img.film-poster-img")
                            ?.attr("data-src"),
                    )
                },
            )
        )

        categories.add(
            Category(
                name = "Trending TV Shows",
                list = document.select("div#trending-tv div.flw-item").map {
                    val info = it.select("div.film-detail > div.fd-infor > span").toInfo()

                    TvShow(
                        id = it.selectFirst("a")
                            ?.attr("href")?.substringAfterLast("-") ?: "",
                        title = it.selectFirst("h3.film-name")
                            ?.text() ?: "",
                        quality = info.quality,
                        rating = info.rating,
                        poster = it.selectFirst("div.film-poster > img.film-poster-img")
                            ?.attr("data-src"),

                        seasons = info.lastEpisode?.let { lastEpisode ->
                            listOf(
                                Season(
                                    id = "",
                                    number = lastEpisode.season,

                                    episodes = listOf(
                                        Episode(
                                            id = "",
                                            number = lastEpisode.episode,
                                        )
                                    )
                                )
                            )
                        } ?: listOf()
                    )
                },
            )
        )

        categories.add(
            Category(
                name = "Latest Movies",
                list = document.select("section.section-id-02")
                    .find { it.selectFirst("h2.cat-heading")?.ownText() == "Latest Movies" }
                    ?.select("div.flw-item")
                    ?.map {
                        val info = it.select("div.film-detail > div.fd-infor > span").toInfo()

                        Movie(
                            id = it.selectFirst("a")
                                ?.attr("href")?.substringAfterLast("-") ?: "",
                            title = it.selectFirst("h3.film-name")
                                ?.text() ?: "",
                            released = info.released,
                            quality = info.quality,
                            rating = info.rating,
                            poster = it.selectFirst("div.film-poster > img.film-poster-img")
                                ?.attr("data-src"),
                        )
                    } ?: listOf(),
            )
        )

        categories.add(
            Category(
                name = "Latest TV Shows",
                list = document.select("section.section-id-02")
                    .find { it.selectFirst("h2.cat-heading")?.ownText() == "Latest TV Shows" }
                    ?.select("div.flw-item")
                    ?.map {
                        val info = it.select("div.film-detail > div.fd-infor > span").toInfo()

                        TvShow(
                            id = it.selectFirst("a")
                                ?.attr("href")?.substringAfterLast("-") ?: "",
                            title = it.selectFirst("h3.film-name")
                                ?.text() ?: "",
                            quality = info.quality,
                            rating = info.rating,
                            poster = it.selectFirst("div.film-poster > img.film-poster-img")
                                ?.attr("data-src"),

                            seasons = info.lastEpisode?.let { lastEpisode ->
                                listOf(
                                    Season(
                                        id = "",
                                        number = lastEpisode.season,

                                        episodes = listOf(
                                            Episode(
                                                id = "",
                                                number = lastEpisode.episode,
                                            )
                                        )
                                    )
                                )
                            } ?: listOf()
                        )
                    } ?: listOf(),
            )
        )

        return categories
    }

    override suspend fun search(query: String, page: Int): List<AppAdapter.Item> {
        if (query.isEmpty()) {
            val document = service.getHome()

            val genres = document.select("div#sidebar_subs_genre li.nav-item a.nav-link")
                .map {
                    Genre(
                        id = it.attr("href")
                            .substringAfterLast("/"),
                        name = it.text(),
                    )
                }
                .sortedBy { it.name }

            return genres
        }

        val document = service.search(query.replace(" ", "-"), page)

        val results = document.select("div.flw-item").map {
            val id = it.selectFirst("a")
                ?.attr("href")?.substringAfterLast("-") ?: ""
            val title = it.selectFirst("h2.film-name")
                ?.text() ?: ""
            val info = it.select("div.film-detail > div.fd-infor > span").toInfo()
            val poster = it.selectFirst("div.film-poster > img.film-poster-img")
                ?.attr("data-src")

            if (it.isMovie()) {
                Movie(
                    id = id,
                    title = title,
                    released = info.released,
                    quality = info.quality,
                    rating = info.rating,
                    poster = poster,
                )
            } else {
                TvShow(
                    id = id,
                    title = title,
                    quality = info.quality,
                    rating = info.rating,
                    poster = poster,

                    seasons = info.lastEpisode?.let { lastEpisode ->
                        listOf(
                            Season(
                                id = "",
                                number = lastEpisode.season,

                                episodes = listOf(
                                    Episode(
                                        id = "",
                                        number = lastEpisode.episode,
                                    )
                                )
                            )
                        )
                    } ?: listOf(),
                )
            }
        }

        return results
    }

    override suspend fun getMovies(page: Int): List<Movie> {
        val document = service.getMovies(page)

        val movies = document.select("div.flw-item").map {
            val info = it.select("div.film-detail > div.fd-infor > span").toInfo()

            Movie(
                id = it.selectFirst("a")
                    ?.attr("href")?.substringAfterLast("-") ?: "",
                title = it.selectFirst("h2.film-name")
                    ?.text() ?: "",
                released = info.released,
                quality = info.quality,
                rating = info.rating,
                poster = it.selectFirst("div.film-poster > img.film-poster-img")
                    ?.attr("data-src"),
            )
        }

        return movies
    }

    override suspend fun getTvShows(page: Int): List<TvShow> {
        val document = service.getTvShows(page)

        val tvShows = document.select("div.flw-item").map {
            val info = it.select("div.film-detail > div.fd-infor > span").toInfo()

            TvShow(
                id = it.selectFirst("a")
                    ?.attr("href")?.substringAfterLast("-") ?: "",
                title = it.selectFirst("h2.film-name")
                    ?.text() ?: "",
                quality = info.quality,
                rating = info.rating,
                poster = it.selectFirst("div.film-poster > img.film-poster-img")
                    ?.attr("data-src"),

                seasons = info.lastEpisode?.let { lastEpisode ->
                    listOf(
                        Season(
                            id = "",
                            number = lastEpisode.season,

                            episodes = listOf(
                                Episode(
                                    id = "",
                                    number = lastEpisode.episode,
                                )
                            )
                        )
                    )
                } ?: listOf()
            )
        }

        return tvShows
    }


    override suspend fun getMovie(id: String): Movie {
        val document = service.getMovie(id)

        val movie = Movie(
            id = id,
            title = document.selectFirst("h2.heading-name")
                ?.text() ?: "",
            overview = document.selectFirst("div.description")
                ?.ownText(),
            released = document.select("div.elements > .row > div > .row-line")
                .find { it.select(".type").text().contains("Released") }
                ?.ownText()?.trim(),
            runtime = document.select("div.elements > .row > div > .row-line")
                .find { it.select(".type").text().contains("Duration") }
                ?.ownText()?.removeSuffix("min")?.trim()?.toIntOrNull(),
            trailer = document.selectFirst("iframe#iframe-trailer")
                ?.attr("data-src")?.substringAfterLast("/")
                ?.let { "https://www.youtube.com/watch?v=${it}" },
            quality = document.selectFirst(".fs-item > .quality")
                ?.text()?.trim(),
            rating = document.selectFirst(".fs-item > .imdb")
                ?.text()?.trim()?.removePrefix("IMDB:")?.toDoubleOrNull(),
            poster = document.selectFirst("div.detail_page-watch img.film-poster-img")
                ?.attr("src"),
            banner = document.selectFirst("div.detail-container > div.cover_follow")
                ?.attr("style")?.substringAfter("background-image: url(")?.substringBefore(");"),

            genres = document.select("div.elements > .row > div > .row-line")
                .find { it.select(".type").text().contains("Genre") }
                ?.select("a")?.map {
                    Genre(
                        id = it.attr("href").substringAfter("/genre/"),
                        name = it.text(),
                    )
                } ?: listOf(),
            cast = document.select("div.elements > .row > div > .row-line")
                .find { it.select(".type").text().contains("Cast") }
                ?.select("a")?.map {
                    People(
                        id = it.attr("href").substringAfter("/cast/"),
                        name = it.text(),
                    )
                } ?: listOf(),
            recommendations = document.select("div.film_related div.flw-item").map {
                val info = it.select("div.film-detail > div.fd-infor > span").toInfo()

                if (it.isMovie()) {
                    Movie(
                        id = it.selectFirst("a")
                            ?.attr("href")?.substringAfterLast("-") ?: "",
                        title = it.selectFirst("h3.film-name")
                            ?.text() ?: "",
                        released = info.released,
                        quality = info.quality,
                        rating = info.rating,
                        poster = it.selectFirst("div.film-poster > img.film-poster-img")
                            ?.attr("data-src"),
                    )
                } else {
                    TvShow(
                        id = it.selectFirst("a")
                            ?.attr("href")?.substringAfterLast("-") ?: "",
                        title = it.selectFirst("h3.film-name")
                            ?.text() ?: "",
                        quality = info.quality,
                        rating = info.rating,
                        poster = it.selectFirst("div.film-poster > img.film-poster-img")
                            ?.attr("data-src"),

                        seasons = info.lastEpisode?.let { lastEpisode ->
                            listOf(
                                Season(
                                    id = "",
                                    number = lastEpisode.season,

                                    episodes = listOf(
                                        Episode(
                                            id = "",
                                            number = lastEpisode.episode,
                                        )
                                    )
                                )
                            )
                        } ?: listOf(),
                    )
                }
            },
        )

        return movie
    }


    override suspend fun getTvShow(id: String): TvShow {
        val document = service.getTvShow(id)

        val tvShow = TvShow(
            id = id,
            title = document.selectFirst("h2.heading-name")
                ?.text() ?: "",
            overview = document.selectFirst("div.description")
                ?.ownText(),
            released = document.select("div.elements > .row > div > .row-line")
                .find { it.select(".type").text().contains("Released") }
                ?.ownText()?.trim(),
            runtime = document.select("div.elements > .row > div > .row-line")
                .find { it.select(".type").text().contains("Duration") }
                ?.ownText()?.removeSuffix("min")?.trim()?.toIntOrNull(),
            trailer = document.selectFirst("iframe#iframe-trailer")
                ?.attr("data-src")?.substringAfterLast("/")
                ?.let { "https://www.youtube.com/watch?v=${it}" },
            quality = document.selectFirst(".fs-item > .quality")
                ?.text()?.trim(),
            rating = document.selectFirst(".fs-item > .imdb")
                ?.text()?.trim()?.removePrefix("IMDB:")?.toDoubleOrNull(),
            poster = document.selectFirst("div.detail_page-watch img.film-poster-img")
                ?.attr("src"),
            banner = document.selectFirst("div.detail-container > div.cover_follow")
                ?.attr("style")?.substringAfter("background-image: url(")?.substringBefore(");"),

            seasons = service.getTvShowSeasons(id)
                .select("div.dropdown-menu.dropdown-menu-model > a")
                .mapIndexed { seasonNumber, seasonElement ->
                    Season(
                        id = seasonElement.attr("data-id"),
                        number = seasonNumber + 1,
                        title = seasonElement.text(),
                    )
                },
            genres = document.select("div.elements > .row > div > .row-line")
                .find { it.select(".type").text().contains("Genre") }
                ?.select("a")?.map {
                    Genre(
                        id = it.attr("href").substringAfter("/genre/"),
                        name = it.text(),
                    )
                } ?: listOf(),
            cast = document.select("div.elements > .row > div > .row-line")
                .find { it.select(".type").text().contains("Cast") }
                ?.select("a")?.map {
                    People(
                        id = it.attr("href").substringAfter("/cast/"),
                        name = it.text(),
                    )
                } ?: listOf(),
            recommendations = document.select("div.film_related div.flw-item").map {
                val info = it.select("div.film-detail > div.fd-infor > span").toInfo()

                if (it.isMovie()) {
                    Movie(
                        id = it.selectFirst("a")
                            ?.attr("href")?.substringAfterLast("-") ?: "",
                        title = it.selectFirst("h3.film-name")
                            ?.text() ?: "",
                        released = info.released,
                        quality = info.quality,
                        rating = info.rating,
                        poster = it.selectFirst("div.film-poster > img.film-poster-img")
                            ?.attr("data-src"),
                    )
                } else {
                    TvShow(
                        id = it.selectFirst("a")
                            ?.attr("href")?.substringAfterLast("-") ?: "",
                        title = it.selectFirst("h3.film-name")
                            ?.text() ?: "",
                        quality = info.quality,
                        rating = info.rating,
                        poster = it.selectFirst("div.film-poster > img.film-poster-img")
                            ?.attr("data-src"),

                        seasons = info.lastEpisode?.let { lastEpisode ->
                            listOf(
                                Season(
                                    id = "",
                                    number = lastEpisode.season,

                                    episodes = listOf(
                                        Episode(
                                            id = "",
                                            number = lastEpisode.episode,
                                        )
                                    )
                                )
                            )
                        } ?: listOf(),
                    )
                }
            },
        )

        return tvShow
    }

    override suspend fun getEpisodesBySeason(seasonId: String): List<Episode> {
        val document = service.getSeasonEpisodes(seasonId)

        val episodes = document.select("div.flw-item.film_single-item.episode-item.eps-item")
            .mapIndexed { episodeNumber, episodeElement ->
                Episode(
                    id = episodeElement.attr("data-id"),
                    number = episodeElement.selectFirst("div.episode-number")
                        ?.text()?.substringAfter("Episode ")?.substringBefore(":")?.toIntOrNull()
                        ?: episodeNumber,
                    title = episodeElement.selectFirst("h3.film-name")
                        ?.text(),
                    poster = episodeElement.selectFirst("img")
                        ?.attr("src"),
                )
            }

        return episodes
    }


    override suspend fun getGenre(id: String, page: Int): Genre {
        val document = service.getGenre(id, page)

        val genre = Genre(
            id = id,
            name = document.selectFirst("h2.cat-heading")
                ?.text()?.removeSuffix(" Movies and TV Shows") ?: "",

            shows = document.select("div.flw-item").map {
                val showId = it.selectFirst("a")
                    ?.attr("href")?.substringAfterLast("-") ?: ""
                val showTitle = it.selectFirst("h2.film-name")
                    ?.text() ?: ""
                val showInfo = it.select("div.film-detail > div.fd-infor > span").toInfo()
                val showPoster = it.selectFirst("div.film-poster > img.film-poster-img")
                    ?.attr("data-src")

                if (it.isMovie()) {
                    Movie(
                        id = showId,
                        title = showTitle,
                        released = showInfo.released,
                        quality = showInfo.quality,
                        rating = showInfo.rating,
                        poster = showPoster,
                    )
                } else {
                    TvShow(
                        id = showId,
                        title = showTitle,
                        quality = showInfo.quality,
                        rating = showInfo.rating,
                        poster = showPoster,

                        seasons = showInfo.lastEpisode?.let { lastEpisode ->
                            listOf(
                                Season(
                                    id = "",
                                    number = lastEpisode.season,

                                    episodes = listOf(
                                        Episode(
                                            id = "",
                                            number = lastEpisode.episode,
                                        )
                                    )
                                )
                            )
                        } ?: listOf(),
                    )
                }
            }
        )

        return genre
    }


    override suspend fun getPeople(id: String, page: Int): People {
        val document = service.getPeople(id, page)

        val people = People(
            id = id,
            name = document.selectFirst("h2.cat-heading")
                ?.text() ?: "",

            filmography = document.select("div.flw-item").map {
                val showId = it.selectFirst("a")
                    ?.attr("href")?.substringAfterLast("-") ?: ""
                val showTitle = it.selectFirst("h2.film-name")
                    ?.text() ?: ""
                val showInfo = it.select("div.film-detail > div.fd-infor > span").toInfo()
                val showPoster = it.selectFirst("div.film-poster > img.film-poster-img")
                    ?.attr("data-src")

                if (it.isMovie()) {
                    Movie(
                        id = showId,
                        title = showTitle,
                        released = showInfo.released,
                        quality = showInfo.quality,
                        rating = showInfo.rating,
                        poster = showPoster,
                    )
                } else {
                    TvShow(
                        id = showId,
                        title = showTitle,
                        quality = showInfo.quality,
                        rating = showInfo.rating,
                        poster = showPoster,

                        seasons = showInfo.lastEpisode?.let { lastEpisode ->
                            listOf(
                                Season(
                                    id = "",
                                    number = lastEpisode.season,

                                    episodes = listOf(
                                        Episode(
                                            id = "",
                                            number = lastEpisode.episode,
                                        )
                                    )
                                )
                            )
                        } ?: listOf(),
                    )
                }
            },
        )

        return people
    }

    override suspend fun getServers(id: String, videoType: Video.Type): List<Video.Server> {
        val servers = when (videoType) {
            is Video.Type.Movie -> service.getMovieServers(id)
            is Video.Type.Episode -> service.getEpisodeServers(id)
        }.select("a")
            .map {
                Video.Server(
                    id = it.attr("data-id"),
                    name = it.selectFirst("span")?.text()?.trim() ?: "",
                )
            }

        if (servers.isEmpty()) throw Exception("No links found")

        return servers
    }

    override suspend fun getVideo(server: Video.Server): Video {
        val link = service.getLink(server.id)

        val sources = service.getEmbed(
            "${BuildConfig.RABBITSTREAM_SOURCE_API}${link.link}&referrer=${URL}"
        )

        val video = Video(
            source = sources.sources.map { it.file }.firstOrNull() ?: "",
            subtitles = sources.tracks
                .filter { it.kind == "captions" }
                .map {
                    Video.Subtitle(
                        label = it.label,
                        file = it.file,
                    )
                }
        )

        return video
    }


    private fun Element.isMovie(): Boolean = this.selectFirst("a")?.attr("href")
        ?.contains("/movie/") ?: false

    private fun Elements.toInfo() = this.map { it.text() }.let {
        object {
            val rating = it.find { s -> s.matches("^\\d(?:\\.\\d)?\$".toRegex()) }?.toDoubleOrNull()

            val quality = it.find { s -> s in listOf("HD", "SD", "CAM", "TS", "HDRip") }

            val released = it.find { s -> s.matches("\\d{4}".toRegex()) }

            val lastEpisode = it.find { s -> s.matches("S\\d+\\s*:E\\d+".toRegex()) }?.let { s ->
                val result = Regex("S(\\d+)\\s*:E(\\d+)").find(s)?.groupValues
                object {
                    val season = result?.getOrNull(1)?.toIntOrNull() ?: 0

                    val episode = result?.getOrNull(2)?.toIntOrNull() ?: 0
                }
            }
        }
    }


    private interface SflixService {

        companion object {
            fun build(): SflixService {
                val client = OkHttpClient.Builder()
                    .readTimeout(30, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .build()

                val retrofit = Retrofit.Builder()
                    .baseUrl(URL)
                    .addConverterFactory(JsoupConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build()

                return retrofit.create(SflixService::class.java)
            }
        }

        @GET("home")
        suspend fun getHome(): Document

        @GET("search/{query}")
        suspend fun search(@Path("query") query: String, @Query("page") page: Int): Document

        @GET("movie")
        suspend fun getMovies(@Query("page") page: Int): Document

        @GET("tv-show")
        suspend fun getTvShows(@Query("page") page: Int): Document


        @GET("movie/free-{id}")
        suspend fun getMovie(@Path("id") id: String): Document

        @GET("ajax/movie/episodes/{id}")
        suspend fun getMovieServers(@Path("id") movieId: String): Document


        @GET("tv/free-{id}")
        suspend fun getTvShow(@Path("id") id: String): Document

        @GET("ajax/v2/tv/seasons/{id}")
        suspend fun getTvShowSeasons(@Path("id") tvShowId: String): Document

        @GET("ajax/v2/season/episodes/{id}")
        suspend fun getSeasonEpisodes(@Path("id") seasonId: String): Document

        @GET("ajax/v2/episode/servers/{id}")
        suspend fun getEpisodeServers(@Path("id") episodeId: String): Document


        @GET("genre/{id}")
        suspend fun getGenre(@Path("id") id: String, @Query("page") page: Int): Document


        @GET("cast/{id}")
        suspend fun getPeople(@Path("id") id: String, @Query("page") page: Int): Document


        @GET("ajax/episode/sources/{id}")
        suspend fun getLink(@Path("id") id: String): Link

        @GET
        suspend fun getEmbed(
            @Url url: String,
        ): Embed


        data class Link(
            val type: String = "",
            val link: String = "",
            val sources: List<String> = listOf(),
            val tracks: List<String> = listOf(),
            val title: String = "",
        )

        data class Embed(
            val sources: List<Source>,
            val tracks: List<Track>,
            val t: Int,
            val server: Int,
        ) {
            data class Source(
                val file: String,
                val type: String,
            )

            data class Track(
                val file: String,
                val label: String,
                val kind: String,
                val default: Boolean?,
            )
        }
    }
}