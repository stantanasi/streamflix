package com.tanasi.streamflix.providers

import com.google.gson.*
import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.extractors.Extractor
import com.tanasi.streamflix.fragments.player.PlayerFragment
import com.tanasi.streamflix.models.*
import com.tanasi.streamflix.utils.retry
import okhttp3.OkHttpClient
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.lang.Integer.min
import java.util.concurrent.TimeUnit

object SflixProvider : Provider {

    override val name = "SFlix"
    override val logo = "https://img.sflix.to/xxrz/400x400/100/66/35/66356c25ce98cb12993249e21742b129/66356c25ce98cb12993249e21742b129.png"
    override val url = "https://sflix.to/"

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
                        ?.text() ?: ""
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

    override suspend fun search(query: String): List<AppAdapter.Item> {
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

        val document = service.search(query.replace(" ", "-"))

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
            }
            else {
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

    override suspend fun getMovies(): List<Movie> {
        val document = service.getMovies()

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

    override suspend fun getTvShows(): List<TvShow> {
        val document = service.getTvShows()

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
                ?.ownText() ?: "",
            released = document.select("div.elements > .row > div > .row-line")
                .find { it?.select(".type")?.text()?.contains("Released") ?: false }
                ?.ownText()?.trim(),
            runtime = document.select("div.elements > .row > div > .row-line")
                .find { it?.select(".type")?.text()?.contains("Duration") ?: false }
                ?.ownText()?.removeSuffix("min")?.trim()?.toIntOrNull(),
            youtubeTrailerId = document.selectFirst("iframe#iframe-trailer")
                ?.attr("data-src")?.substringAfterLast("/"),
            quality = document.selectFirst(".fs-item > .quality")
                ?.text()?.trim(),
            rating = document.selectFirst(".fs-item > .imdb")
                ?.text()?.trim()?.removePrefix("IMDB:")?.toDoubleOrNull(),
            poster = document.selectFirst("div.detail_page-watch img.film-poster-img")
                ?.attr("src"),
            banner = document.selectFirst("div.detail-container > div.cover_follow")
                ?.attr("style")?.substringAfter("background-image: url(")?.substringBefore(");"),

            genres = document.select("div.elements > .row > div > .row-line")
                .find { it?.select(".type")?.text()?.contains("Genre") ?: false }
                ?.select("a")?.map {
                    Genre(
                        id = it.attr("href").substringAfter("/genre/"),
                        name = it.text(),
                    )
                } ?: listOf(),
            cast = document.select("div.elements > .row > div > .row-line")
                .find { it?.select(".type")?.text()?.contains("Casts") ?: false }
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
                ?.ownText() ?: "",
            released = document.select("div.elements > .row > div > .row-line")
                .find { it?.select(".type")?.text()?.contains("Released") ?: false }
                ?.ownText()?.trim(),
            runtime = document.select("div.elements > .row > div > .row-line")
                .find { it?.select(".type")?.text()?.contains("Duration") ?: false }
                ?.ownText()?.removeSuffix("min")?.trim()?.toIntOrNull(),
            youtubeTrailerId = document.selectFirst("iframe#iframe-trailer")
                ?.attr("data-src")?.substringAfterLast("/"),
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
                .find { it?.select(".type")?.text()?.contains("Genre") ?: false }
                ?.select("a")?.map {
                    Genre(
                        id = it.attr("href").substringAfter("/genre/"),
                        name = it.text(),
                    )
                } ?: listOf(),
            cast = document.select("div.elements > .row > div > .row-line")
                .find { it?.select(".type")?.text()?.contains("Casts") ?: false }
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
                        ?.text() ?: "",
                    poster = episodeElement.selectFirst("img")
                        ?.attr("src"),
                )
            }

        return episodes
    }


    override suspend fun getGenre(id: String): Genre {
        val document = service.getGenre(id)

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


    override suspend fun getPeople(id: String): People {
        val document = service.getPeople(id)

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


    override suspend fun getVideo(id: String, videoType: PlayerFragment.VideoType): Video {
        val servers = when (videoType) {
            is PlayerFragment.VideoType.Movie -> service.getMovieServers(id)
            is PlayerFragment.VideoType.Episode -> service.getEpisodeServers(id)
        }.select("a").map {
            object {
                val id = it.attr("data-id")
                val name = it.selectFirst("span")?.text()?.trim() ?: ""
            }
        }

        if (servers.isEmpty()) throw Exception("No links found")

        val video = retry(min(servers.size, 2)) { attempt ->
            val link = service.getLink(servers.getOrNull(attempt - 1)?.id ?: "")

            Extractor.extract(link.link)
        }

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


    interface SflixService {

        companion object {
            fun build(): SflixService {
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

                return retrofit.create(SflixService::class.java)
            }
        }


        @GET("home")
        suspend fun getHome(): Document

        @GET("search/{query}")
        suspend fun search(@Path("query") query: String): Document

        @GET("movie")
        suspend fun getMovies(): Document

        @GET("tv-show")
        suspend fun getTvShows(): Document


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
        suspend fun getGenre(@Path("id") id: String): Document


        @GET("cast/{id}")
        suspend fun getPeople(@Path("id") id: String): Document


        @GET("ajax/get_link/{id}")
        suspend fun getLink(@Path("id") id: String): Link


        data class Link(
            val type: String = "",
            val link: String = "",
            val sources: List<String> = listOf(),
            val tracks: List<String> = listOf(),
            val title: String = "",
        )
    }
}