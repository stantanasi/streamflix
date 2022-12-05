package com.tanasi.sflix.providers

import android.net.Uri
import android.util.Base64
import com.google.gson.*
import com.tanasi.retrofit_jsoup.converter.JsoupConverterFactory
import com.tanasi.sflix.fragments.player.PlayerFragment
import com.tanasi.sflix.models.*
import com.tanasi.sflix.utils.retry
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.lang.Integer.min
import java.lang.reflect.Type
import java.net.URI
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object SflixProvider : Provider {

    override var name = "SFlix"
    override var logo = "https://img.sflix.to/xxrz/400x400/100/66/35/66356c25ce98cb12993249e21742b129/66356c25ce98cb12993249e21742b129.png"

    private val service = SflixService.build()


    override suspend fun getHome(): List<Category> {
        val document = service.getHome()

        val categories = mutableListOf<Category>()

        categories.add(
            Category(
                name = "Featured",
                list = document.select("div.swiper-wrapper > div.swiper-slide").map {
                    val id = it.selectFirst("a")
                        ?.attr("href")?.substringAfterLast("-") ?: ""
                    val title = it.selectFirst("h2.film-title")
                        ?.text() ?: ""
                    val overview = it.selectFirst("p.sc-desc")
                        ?.text() ?: ""
                    val poster = it.selectFirst("img.film-poster-img")
                        ?.attr("src")
                    val banner = it.selectFirst("div.slide-photo img")
                        ?.attr("src")

                    when {
                        it.isMovie() -> {
                            val info = it.select("div.sc-detail > div.scd-item").toInfo()

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
                        }
                        else -> {
                            val info = it.select("div.sc-detail > div.scd-item").toInfo()

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

    override suspend fun search(query: String): List<Show> {
        if (query.isEmpty()) return listOf()

        val document = service.search(query.replace(" ", "-"))

        val results = document.select("div.flw-item").map {
            val id = it.selectFirst("a")
                ?.attr("href")?.substringAfterLast("-") ?: ""
            val title = it.selectFirst("h2.film-name")
                ?.text() ?: ""
            val poster = it.selectFirst("div.film-poster > img.film-poster-img")
                ?.attr("data-src")

            when {
                it.isMovie() -> {
                    val info = it.select("div.film-detail > div.fd-infor > span").toInfo()

                    Movie(
                        id = id,
                        title = title,
                        released = info.released,
                        quality = info.quality,
                        rating = info.rating,
                        poster = poster,
                    )
                }
                else -> {
                    val info = it.select("div.film-detail > div.fd-infor > span").toInfo()

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
        val document = service.getMovieById(id)

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
                when {
                    it.isMovie() -> {
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
                    }
                    else -> {
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
                            } ?: listOf(),
                        )
                    }
                }
            },
        )

        return movie
    }


    override suspend fun getTvShow(id: String): TvShow {
        val document = service.getTvShowById(id)

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

            seasons = service.getTvShowSeasonsById(id)
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
                when {
                    it.isMovie() -> {
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
                    }
                    else -> {
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
                            } ?: listOf(),
                        )
                    }
                }
            },
        )

        return tvShow
    }

    override suspend fun getSeasonEpisodes(seasonId: String): List<Episode> {
        val document = service.getSeasonEpisodesById(seasonId)

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


    override suspend fun getPeople(id: String): People {
        val document = service.getPeopleBySlug(id)

        val people = People(
            id = id,
            name = document.selectFirst("h2.cat-heading")
                ?.text() ?: "",

            filmography = document.select("div.flw-item").map {
                val showId = it.selectFirst("a")
                    ?.attr("href")?.substringAfterLast("-") ?: ""
                val showTitle = it.selectFirst("h2.film-name")
                    ?.text() ?: ""
                val showPoster = it.selectFirst("div.film-poster > img.film-poster-img")
                    ?.attr("data-src")

                when {
                    it.isMovie() -> {
                        val info = it.select("div.film-detail > div.fd-infor > span").toInfo()

                        Movie(
                            id = showId,
                            title = showTitle,
                            released = info.released,
                            quality = info.quality,
                            rating = info.rating,
                            poster = showPoster,
                        )
                    }
                    else -> {
                        val info = it.select("div.film-detail > div.fd-infor > span").toInfo()

                        TvShow(
                            id = showId,
                            title = showTitle,
                            quality = info.quality,
                            rating = info.rating,
                            poster = showPoster,

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
            },
        )

        return people
    }


    override suspend fun getVideo(id: String, videoType: PlayerFragment.VideoType): Video {
        val servers = when (videoType) {
            is PlayerFragment.VideoType.Movie -> service.getMovieServersById(id)
            is PlayerFragment.VideoType.Episode -> service.getEpisodeServersById(id)
        }.select("a").map {
            object {
                val id = it.attr("data-id")
                val name = it.selectFirst("span")?.text()?.trim() ?: ""
            }
        }

        val video = retry(min(servers.size, 2)) { attempt ->
            val link = service.getLink(servers.getOrNull(attempt - 1)?.id ?: "")

            val response = service.getSources(
                url = link.link
                    .substringBeforeLast("/")
                    .replace("/embed", "/ajax/embed")
                    .plus("/getSources"),
                id = link.link.substringAfterLast("/").substringBefore("?"),
            )

            val sources = when (response) {
                is SflixService.Sources -> response
                is SflixService.Sources.Encrypted -> response.decrypt(
                    secret = service.getSourceEncryptedKey(
                        domain = URI(link.link).host.substringBefore("."),
                    ).text()
                )
            }

            Video(
                sources = sources.sources.map { it.file },
                subtitles = sources.tracks
                    .filter { it.kind == "captions" }
                    .map {
                        Video.Subtitle(
                            label = it.label,
                            file = it.file,
                            default = it.default,
                        )
                    }
            )
        }

        return video
    }


    private fun Element.isMovie(): Boolean = this.selectFirst("a")?.attr("href")
        ?.contains("/movie/") ?: false

    private fun Elements.toInfo() = this.map { it.text() }.let {
        object {
            val rating = it.find { s -> s.matches("^\\d(?:\\.\\d)?\$".toRegex()) }?.toDoubleOrNull()

            val quality = it.find { s -> s in listOf("HD", "SD", "CAM") }

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
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://sflix.to/")
                    .addConverterFactory(JsoupConverterFactory.create())
                    .addConverterFactory(
                        GsonConverterFactory.create(
                            GsonBuilder()
                                .registerTypeAdapter(
                                    SourcesResponse::class.java,
                                    SourcesResponse.Deserializer(),
                                )
                                .create()
                        )
                    )
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
        suspend fun getMovieById(@Path("id") id: String): Document

        @GET("ajax/movie/episodes/{id}")
        suspend fun getMovieServersById(@Path("id") movieId: String): Document


        @GET("tv/free-{id}")
        suspend fun getTvShowById(@Path("id") id: String): Document

        @GET("ajax/v2/tv/seasons/{id}")
        suspend fun getTvShowSeasonsById(@Path("id") tvShowId: String): Document

        @GET("ajax/v2/season/episodes/{id}")
        suspend fun getSeasonEpisodesById(@Path("id") seasonId: String): Document

        @GET("ajax/v2/episode/servers/{id}")
        suspend fun getEpisodeServersById(@Path("id") episodeId: String): Document


        @GET("cast/{slug}")
        suspend fun getPeopleBySlug(@Path("slug") slug: String): Document


        @GET("ajax/get_link/{id}")
        suspend fun getLink(@Path("id") id: String): Link

        @GET
        @Headers(
            "Accept: */*",
            "Accept-Language: en-US,en;q=0.5",
            "Connection: keep-alive",
            "referer: https://sflix.to",
            "TE: trailers",
            "X-Requested-With: XMLHttpRequest",
        )
        suspend fun getSources(
            @Url url: String,
            @Query("id") id: String,
        ): SourcesResponse

        @GET("https://raw.githubusercontent.com/consumet/rapidclown/{domain}/key.txt")
        suspend fun getSourceEncryptedKey(@Path("domain") domain: String): Document


        data class Link(
            val type: String = "",
            val link: String = "",
            val sources: List<String> = listOf(),
            val tracks: List<String> = listOf(),
            val title: String = "",
        )

        sealed class SourcesResponse {
            class Deserializer : JsonDeserializer<SourcesResponse> {
                override fun deserialize(
                    json: JsonElement?,
                    typeOfT: Type?,
                    context: JsonDeserializationContext?
                ): SourcesResponse {
                    val jsonObject = json?.asJsonObject ?: JsonObject()

                    return when (jsonObject.get("sources")?.isJsonArray ?: false) {
                        true -> Gson().fromJson(json, Sources::class.java)
                        false -> Gson().fromJson(json, Sources.Encrypted::class.java)
                    }
                }
            }
        }

        data class Sources(
            val sources: List<Source> = listOf(),
            val sourcesBackup: List<Source> = listOf(),
            val tracks: List<Track> = listOf(),
            val server: Int? = null,
        ) : SourcesResponse() {

            data class Encrypted(
                val sources: String,
                val sourcesBackup: String? = null,
                val tracks: List<Track> = listOf(),
                val server: Int? = null,
            ) : SourcesResponse() {
                fun decrypt(secret: String): Sources {
                    fun decryptSourceUrl(decryptionKey: ByteArray, sourceUrl: String): String {
                        val cipherData = Base64.decode(sourceUrl, Base64.DEFAULT)
                        val encrypted = cipherData.copyOfRange(16, cipherData.size)
                        val aesCBC = Cipher.getInstance("AES/CBC/PKCS5Padding")!!

                        aesCBC.init(
                            Cipher.DECRYPT_MODE,
                            SecretKeySpec(
                                decryptionKey.copyOfRange(0, 32),
                                "AES"
                            ),
                            IvParameterSpec(decryptionKey.copyOfRange(32, decryptionKey.size))
                        )
                        val decryptedData = aesCBC.doFinal(encrypted)
                        return String(decryptedData, StandardCharsets.UTF_8)
                    }

                    fun generateKey(salt: ByteArray, secret: ByteArray): ByteArray {
                        fun md5(input: ByteArray) = MessageDigest.getInstance("MD5").digest(input)

                        var key = md5(secret + salt)
                        var currentKey = key
                        while (currentKey.size < 48) {
                            key = md5(key + secret + salt)
                            currentKey += key
                        }
                        return currentKey
                    }

                    val decrypted = decryptSourceUrl(
                        generateKey(
                            Base64.decode(sources, Base64.DEFAULT).copyOfRange(8, 16),
                            secret.toByteArray(),
                        ),
                        sources,
                    )

                    return Sources(
                        sources = Gson().fromJson(decrypted, Array<Source>::class.java).toList(),
                        tracks = tracks
                    )
                }
            }

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
}