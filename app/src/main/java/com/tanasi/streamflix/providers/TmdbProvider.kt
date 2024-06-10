package com.tanasi.streamflix.providers

import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.extractors.Extractor
import com.tanasi.streamflix.extractors.VidsrcToExtractor
import com.tanasi.streamflix.models.Category
import com.tanasi.streamflix.models.Episode
import com.tanasi.streamflix.models.Genre
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.models.People
import com.tanasi.streamflix.models.Season
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.models.Video
import com.tanasi.streamflix.utils.TMDb3
import com.tanasi.streamflix.utils.TMDb3.original
import com.tanasi.streamflix.utils.TMDb3.w500
import com.tanasi.streamflix.utils.safeSubList
import java.util.Calendar

object TmdbProvider : Provider {

    override val name = "TMDb"
    override val logo =
        "https://upload.wikimedia.org/wikipedia/commons/thumb/8/89/Tmdb.new.logo.svg/1280px-Tmdb.new.logo.svg.png"

    override suspend fun getHome(): List<Category> {
        val categories = mutableListOf<Category>()

        val trending = TMDb3.Trending.all(TMDb3.Params.TimeWindow.DAY)

        categories.add(
            Category(
                name = Category.FEATURED,
                list = trending.results.safeSubList(0, 5).mapNotNull { multi ->
                    when (multi.mediaType) {
                        TMDb3.MultiItem.MediaType.MOVIE -> Movie(
                            id = multi.id.toString(),
                            title = multi.title ?: "",
                            overview = multi.overview,
//                                released = multi.releasedDate,
                            rating = multi.voteAverage.toDouble(),
                            poster = multi.posterPath?.w500,
                            banner = multi.backdropPath?.original,
                        )

                        TMDb3.MultiItem.MediaType.TV -> TvShow(
                            id = multi.id.toString(),
                            title = multi.name ?: "",
                            overview = multi.overview,
//                                released = multi.firstAirDate,
                            rating = multi.voteAverage.toDouble(),
                            poster = multi.posterPath?.w500,
                            banner = multi.backdropPath?.original,
                        )

                        else -> null
                    }
                }
            )
        )

        categories.add(
            Category(
                name = "Trending",
                list = trending.results.safeSubList(5, trending.results.size).mapNotNull { multi ->
                    when (multi.mediaType) {
                        TMDb3.MultiItem.MediaType.MOVIE -> Movie(
                            id = multi.id.toString(),
                            title = multi.title ?: "",
                            overview = multi.overview,
//                            released = multi.releasedDate,
                            rating = multi.voteAverage.toDouble(),
                            poster = multi.posterPath?.w500,
                            banner = multi.backdropPath?.original,
                        )

                        TMDb3.MultiItem.MediaType.TV -> TvShow(
                            id = multi.id.toString(),
                            title = multi.name ?: "",
                            overview = multi.overview,
//                            released = multi.firstAirDate,
                            rating = multi.voteAverage.toDouble(),
                            poster = multi.posterPath?.w500,
                            banner = multi.backdropPath?.original,
                        )

                        else -> null
                    }
                }
            )
        )

        categories.add(
            Category(
                name = "Popular Movies",
                list = TMDb3.MovieLists.popular().results.map { movie ->
                    Movie(
                        id = movie.id.toString(),
                        title = movie.title,
                        overview = movie.overview,
                        released = movie.releaseDate,
                        rating = movie.voteAverage.toDouble(),
                        poster = movie.posterPath?.w500,
                        banner = movie.backdropPath?.original,
                    )
                }
            )
        )

        categories.add(
            Category(
                name = "Popular TV Shows",
                list = TMDb3.TvSeriesLists.popular().results.map { tv ->
                    TvShow(
                        id = tv.id.toString(),
                        title = tv.name,
                        overview = tv.overview,
                        released = tv.firstAirDate,
                        rating = tv.voteAverage.toDouble(),
                        poster = tv.posterPath?.w500,
                        banner = tv.backdropPath?.original,
                    )
                }
            )
        )

        categories.add(
            Category(
                name = "Airing Today TV Shows",
                list = TMDb3.TvSeriesLists.airingToday().results.map { tv ->
                    TvShow(
                        id = tv.id.toString(),
                        title = tv.name,
                        overview = tv.overview,
                        released = tv.firstAirDate,
                        rating = tv.voteAverage.toDouble(),
                        poster = tv.posterPath?.w500,
                        banner = tv.backdropPath?.original,
                    )
                }
            )
        )

        categories.add(
            Category(
                name = "Netflix",
                list = TMDb3.Discover.tv(
                    withNetworks = TMDb3.Params.WithBuilder(TMDb3.Network.NetworkId.NETFLIX),
                ).results.map { tv ->
                    TvShow(
                        id = tv.id.toString(),
                        title = tv.name,
                        overview = tv.overview,
                        released = tv.firstAirDate,
                        rating = tv.voteAverage.toDouble(),
                        poster = tv.posterPath?.w500,
                        banner = tv.backdropPath?.original,
                    )
                }
            )
        )

        categories.add(
            Category(
                name = "Amazon",
                list = TMDb3.Discover.tv(
                    withNetworks = TMDb3.Params.WithBuilder(TMDb3.Network.NetworkId.AMAZON),
                ).results.map { tv ->
                    TvShow(
                        id = tv.id.toString(),
                        title = tv.name,
                        overview = tv.overview,
                        released = tv.firstAirDate,
                        rating = tv.voteAverage.toDouble(),
                        poster = tv.posterPath?.w500,
                        banner = tv.backdropPath?.original,
                    )
                }
            )
        )

        categories.add(
            Category(
                name = "Disney+",
                list = TMDb3.Discover.tv(
                    withNetworks = TMDb3.Params.WithBuilder(TMDb3.Network.NetworkId.DISNEY_PLUS),
                ).results.map { tv ->
                    TvShow(
                        id = tv.id.toString(),
                        title = tv.name,
                        overview = tv.overview,
                        released = tv.firstAirDate,
                        rating = tv.voteAverage.toDouble(),
                        poster = tv.posterPath?.w500,
                        banner = tv.backdropPath?.original,
                    )
                }
            )
        )

        categories.add(
            Category(
                name = "Hulu",
                list = TMDb3.Discover.tv(
                    withNetworks = TMDb3.Params.WithBuilder(TMDb3.Network.NetworkId.HULU),
                ).results.map { tv ->
                    TvShow(
                        id = tv.id.toString(),
                        title = tv.name,
                        overview = tv.overview,
                        released = tv.firstAirDate,
                        rating = tv.voteAverage.toDouble(),
                        poster = tv.posterPath?.w500,
                        banner = tv.backdropPath?.original,
                    )
                }
            )
        )

        categories.add(
            Category(
                name = "Apple TV+",
                list = TMDb3.Discover.tv(
                    withNetworks = TMDb3.Params.WithBuilder(TMDb3.Network.NetworkId.APPLE_TV),
                ).results.map { tv ->
                    TvShow(
                        id = tv.id.toString(),
                        title = tv.name,
                        overview = tv.overview,
                        released = tv.firstAirDate,
                        rating = tv.voteAverage.toDouble(),
                        poster = tv.posterPath?.w500,
                        banner = tv.backdropPath?.original,
                    )
                }
            )
        )

        categories.add(
            Category(
                name = "HBO",
                list = TMDb3.Discover.tv(
                    withNetworks = TMDb3.Params.WithBuilder(TMDb3.Network.NetworkId.HBO),
                ).results.map { tv ->
                    TvShow(
                        id = tv.id.toString(),
                        title = tv.name,
                        overview = tv.overview,
                        released = tv.firstAirDate,
                        rating = tv.voteAverage.toDouble(),
                        poster = tv.posterPath?.w500,
                        banner = tv.backdropPath?.original,
                    )
                }
            )
        )

        categories.add(
            Category(
                name = "Paramount+",
                list = TMDb3.Discover.tv(
                    withNetworks = TMDb3.Params.WithBuilder(TMDb3.Network.NetworkId.PARAMOUNT_PLUS),
                ).results.map { tv ->
                    TvShow(
                        id = tv.id.toString(),
                        title = tv.name,
                        overview = tv.overview,
                        released = tv.firstAirDate,
                        rating = tv.voteAverage.toDouble(),
                        poster = tv.posterPath?.w500,
                        banner = tv.backdropPath?.original,
                    )
                }
            )
        )

        categories.add(
            Category(
                name = "Peacock",
                list = TMDb3.Discover.tv(
                    withNetworks = TMDb3.Params.WithBuilder(TMDb3.Network.NetworkId.PEACOCK),
                ).results.map { tv ->
                    TvShow(
                        id = tv.id.toString(),
                        title = tv.name,
                        overview = tv.overview,
                        released = tv.firstAirDate,
                        rating = tv.voteAverage.toDouble(),
                        poster = tv.posterPath?.w500,
                        banner = tv.backdropPath?.original,
                    )
                }
            )
        )

        categories.add(
            Category(
                name = "Top Rated Movies",
                list = TMDb3.MovieLists.topRated().results.map { movie ->
                    Movie(
                        id = movie.id.toString(),
                        title = movie.title,
                        overview = movie.overview,
                        released = movie.releaseDate,
                        rating = movie.voteAverage.toDouble(),
                        poster = movie.posterPath?.w500,
                        banner = movie.backdropPath?.original,
                    )
                }
            )
        )

        categories.add(
            Category(
                name = "Top Rated TV Shows",
                list = TMDb3.TvSeriesLists.topRated().results.map { tv ->
                    TvShow(
                        id = tv.id.toString(),
                        title = tv.name,
                        overview = tv.overview,
                        released = tv.firstAirDate,
                        rating = tv.voteAverage.toDouble(),
                        poster = tv.posterPath?.w500,
                        banner = tv.backdropPath?.original,
                    )
                }
            )
        )

        categories.add(
            Category(
                name = "Korean Shows",
                list = TMDb3.Discover.tv(
                    withOriginalLanguage = TMDb3.Params.WithBuilder("ko")
                ).results.map { tv ->
                    TvShow(
                        id = tv.id.toString(),
                        title = tv.name,
                        overview = tv.overview,
                        released = tv.firstAirDate,
                        rating = tv.voteAverage.toDouble(),
                        poster = tv.posterPath?.w500,
                        banner = tv.backdropPath?.original,
                    )
                }
            )
        )

        categories.add(
            Category(
                name = "Airing Today Anime",
                list = TMDb3.Discover.tv(
                    airDate = TMDb3.Params.Range(
                        gte = Calendar.getInstance(),
                        lte = Calendar.getInstance().apply {
                            add(Calendar.WEEK_OF_YEAR, 1)
                        }
                    ),
                    withKeywords = TMDb3.Params.WithBuilder(TMDb3.Keyword.KeywordId.ANIME)
                        .or(TMDb3.Keyword.KeywordId.BASED_ON_ANIME),
                ).results.map { tv ->
                    TvShow(
                        id = tv.id.toString(),
                        title = tv.name,
                        overview = tv.overview,
                        released = tv.firstAirDate,
                        rating = tv.voteAverage.toDouble(),
                        poster = tv.posterPath?.w500,
                        banner = tv.backdropPath?.original,
                    )
                }
            )
        )

        categories.add(
            Category(
                name = "On The Air Anime",
                list = TMDb3.Discover.tv(
                    airDate = TMDb3.Params.Range(
                        gte = Calendar.getInstance(),
                        lte = Calendar.getInstance()
                    ),
                    withKeywords = TMDb3.Params.WithBuilder(TMDb3.Keyword.KeywordId.ANIME)
                        .or(TMDb3.Keyword.KeywordId.BASED_ON_ANIME),
                ).results.map { tv ->
                    TvShow(
                        id = tv.id.toString(),
                        title = tv.name,
                        overview = tv.overview,
                        released = tv.firstAirDate,
                        rating = tv.voteAverage.toDouble(),
                        poster = tv.posterPath?.w500,
                        banner = tv.backdropPath?.original,
                    )
                }
            )
        )

        categories.add(
            Category(
                name = "Anime",
                list = TMDb3.Discover.tv(
                    withKeywords = TMDb3.Params.WithBuilder(TMDb3.Keyword.KeywordId.ANIME)
                        .or(TMDb3.Keyword.KeywordId.BASED_ON_ANIME),
                ).results.map { tv ->
                    TvShow(
                        id = tv.id.toString(),
                        title = tv.name,
                        overview = tv.overview,
                        released = tv.firstAirDate,
                        rating = tv.voteAverage.toDouble(),
                        poster = tv.posterPath?.w500,
                        banner = tv.backdropPath?.original,
                    )
                }
            )
        )

        categories.add(
            Category(
                name = "Anime Movies",
                list = TMDb3.Discover.movie(
                    withKeywords = TMDb3.Params.WithBuilder(TMDb3.Keyword.KeywordId.ANIME)
                        .or(TMDb3.Keyword.KeywordId.BASED_ON_ANIME),
                ).results.map { movie ->
                    Movie(
                        id = movie.id.toString(),
                        title = movie.title,
                        overview = movie.overview,
                        released = movie.releaseDate,
                        rating = movie.voteAverage.toDouble(),
                        poster = movie.posterPath?.w500,
                        banner = movie.backdropPath?.original,
                    )
                }
            )
        )

        return categories
    }

    override suspend fun search(query: String, page: Int): List<AppAdapter.Item> {
        if (query.isEmpty()) {
            val genres = listOf(
                TMDb3.Genres.movieList(),
                TMDb3.Genres.tvList(),
            ).flatMap { it.genres }.map {
                Genre(
                    id = it.id.toString(),
                    name = it.name,
                )
            }

            return genres
        }

        val results = TMDb3.Search.multi(query, page = page).results.mapNotNull { multi ->
            when (multi.mediaType) {
                TMDb3.MultiItem.MediaType.MOVIE -> Movie(
                    id = multi.id.toString(),
                    title = multi.title ?: "",
                    overview = multi.overview,
//                        released = multi.releasedDate,
                    rating = multi.voteAverage.toDouble(),
                    poster = multi.posterPath?.w500,
                    banner = multi.backdropPath?.original,
                )

                TMDb3.MultiItem.MediaType.TV -> TvShow(
                    id = multi.id.toString(),
                    title = multi.name ?: "",
                    overview = multi.overview,
//                        released = multi.firstAirDate,
                    rating = multi.voteAverage.toDouble(),
                    poster = multi.posterPath?.w500,
                    banner = multi.backdropPath?.original,
                )

                else -> null
            }
        }

        return results
    }

    override suspend fun getMovies(page: Int): List<Movie> {
        val movies = TMDb3.MovieLists.popular(page = page).results.map { movie ->
            Movie(
                id = movie.id.toString(),
                title = movie.title,
                overview = movie.overview,
                released = movie.releaseDate,
                rating = movie.voteAverage.toDouble(),
                poster = movie.posterPath?.w500,
                banner = movie.backdropPath?.original,
            )
        }

        return movies
    }

    override suspend fun getTvShows(page: Int): List<TvShow> {
        val tvShows = TMDb3.TvSeriesLists.popular(page = page).results.map { tv ->
            TvShow(
                id = tv.id.toString(),
                title = tv.name,
                overview = tv.overview,
                released = tv.firstAirDate,
                rating = tv.voteAverage.toDouble(),
                poster = tv.posterPath?.w500,
                banner = tv.backdropPath?.original,
            )
        }

        return tvShows
    }

    override suspend fun getMovie(id: String): Movie {
        val movie = TMDb3.Movies.details(
            movieId = id.toInt(),
            appendToResponse = listOf(
                TMDb3.Params.AppendToResponse.Movie.CREDITS,
                TMDb3.Params.AppendToResponse.Movie.RECOMMENDATIONS,
                TMDb3.Params.AppendToResponse.Movie.VIDEOS,
            )
        ).let { movie ->
            Movie(
                id = movie.id.toString(),
                title = movie.title,
                overview = movie.overview,
                released = movie.releaseDate,
                runtime = movie.runtime,
                trailer = movie.videos?.results
                    ?.sortedBy { it.publishedAt ?: "" }
                    ?.firstOrNull { it.site == TMDb3.Video.VideoSite.YOUTUBE }
                    ?.let { "https://www.youtube.com/watch?v=${it.key}" },
                rating = movie.voteAverage.toDouble(),
                poster = movie.posterPath?.original,
                banner = movie.backdropPath?.original,

                genres = movie.genres.map { genre ->
                    Genre(
                        genre.id.toString(),
                        genre.name,
                    )
                },
                cast = movie.credits?.cast?.map { cast ->
                    People(
                        id = cast.id.toString(),
                        name = cast.name,
                        image = cast.profilePath?.w500,
                    )
                } ?: listOf(),
                recommendations = movie.recommendations?.results?.mapNotNull { multi ->
                    when (multi.mediaType) {
                        TMDb3.MultiItem.MediaType.MOVIE -> Movie(
                            id = multi.id.toString(),
                            title = multi.title ?: "",
                            overview = multi.overview,
//                            released = multi.releasedDate,
                            rating = multi.voteAverage.toDouble(),
                            poster = multi.posterPath?.w500,
                            banner = multi.backdropPath?.original,
                        )

                        TMDb3.MultiItem.MediaType.TV -> TvShow(
                            id = multi.id.toString(),
                            title = multi.name ?: "",
                            overview = multi.overview,
//                            released = multi.firstAirDate,
                            rating = multi.voteAverage.toDouble(),
                            poster = multi.posterPath?.w500,
                            banner = multi.backdropPath?.original,
                        )

                        else -> null
                    }
                } ?: listOf(),
            )
        }

        return movie
    }

    override suspend fun getTvShow(id: String): TvShow {
        val tvShow = TMDb3.TvSeries.details(
            seriesId = id.toInt(),
            appendToResponse = listOf(
                TMDb3.Params.AppendToResponse.Tv.CREDITS,
                TMDb3.Params.AppendToResponse.Tv.RECOMMENDATIONS,
                TMDb3.Params.AppendToResponse.Tv.VIDEOS,
            )
        ).let { tv ->
            TvShow(
                id = tv.id.toString(),
                title = tv.name,
                overview = tv.overview,
                released = tv.firstAirDate,
                trailer = tv.videos?.results
                    ?.sortedBy { it.publishedAt ?: "" }
                    ?.firstOrNull { it.site == TMDb3.Video.VideoSite.YOUTUBE }
                    ?.let { "https://www.youtube.com/watch?v=${it.key}" },
                rating = tv.voteAverage.toDouble(),
                poster = tv.posterPath?.original,
                banner = tv.backdropPath?.original,

                seasons = tv.seasons.map { season ->
                    Season(
                        id = "${tv.id}-${season.seasonNumber}",
                        number = season.seasonNumber,
                        title = season.name,
                        poster = season.posterPath?.w500,
                    )
                },
                genres = tv.genres.map { genre ->
                    Genre(
                        genre.id.toString(),
                        genre.name,
                    )
                },
                cast = tv.credits?.cast?.map { cast ->
                    People(
                        id = cast.id.toString(),
                        name = cast.name,
                        image = cast.profilePath?.w500,
                    )
                } ?: listOf(),
                recommendations = tv.recommendations?.results?.mapNotNull { multi ->
                    when (multi.mediaType) {
                        TMDb3.MultiItem.MediaType.MOVIE -> Movie(
                            id = multi.id.toString(),
                            title = multi.title ?: "",
                            overview = multi.overview,
//                            released = multi.releasedDate,
                            rating = multi.voteAverage.toDouble(),
                            poster = multi.posterPath?.w500,
                            banner = multi.backdropPath?.original,
                        )

                        TMDb3.MultiItem.MediaType.TV -> TvShow(
                            id = multi.id.toString(),
                            title = multi.name ?: "",
                            overview = multi.overview,
//                            released = multi.firstAirDate,
                            rating = multi.voteAverage.toDouble(),
                            poster = multi.posterPath?.w500,
                            banner = multi.backdropPath?.original,
                        )

                        else -> null
                    }
                } ?: listOf(),
            )
        }

        return tvShow
    }

    override suspend fun getEpisodesBySeason(seasonId: String): List<Episode> {
        val (tvShowId, seasonNumber) = seasonId.split("-")

        val episodes = TMDb3.TvSeasons.details(
            seriesId = tvShowId.toInt(),
            seasonNumber = seasonNumber.toInt(),
        ).episodes?.map {
            Episode(
                id = it.id.toString(),
                number = it.episodeNumber,
                title = it.name ?: "",
                released = it.airDate,
                poster = it.stillPath?.w500,
            )
        } ?: listOf()

        return episodes
    }

    override suspend fun getGenre(id: String, page: Int): Genre {
        fun <T> List<T>.mix(other: List<T>): List<T> {
            return sequence {
                val first = iterator()
                val second = other.iterator()
                while (first.hasNext() && second.hasNext()) {
                    yield(first.next())
                    yield(second.next())
                }

                yieldAll(first)
                yieldAll(second)
            }.toList()
        }

        val genre = Genre(
            id = id,
            name = "",

            shows = TMDb3.Discover.movie(
                page = page,
                withGenres = TMDb3.Params.WithBuilder(id),
            ).results.map { movie ->
                Movie(
                    id = movie.id.toString(),
                    title = movie.title,
                    overview = movie.overview,
                    released = movie.releaseDate,
                    rating = movie.voteAverage.toDouble(),
                    poster = movie.posterPath?.w500,
                    banner = movie.backdropPath?.original,
                )
            }.mix(TMDb3.Discover.tv(
                page = page,
                withGenres = TMDb3.Params.WithBuilder(id)
            ).results.map { tv ->
                TvShow(
                    id = tv.id.toString(),
                    title = tv.name,
                    overview = tv.overview,
                    released = tv.firstAirDate,
                    rating = tv.voteAverage.toDouble(),
                    poster = tv.posterPath?.w500,
                    banner = tv.backdropPath?.original,
                )
            })
        )

        return genre
    }

    override suspend fun getPeople(id: String, page: Int): People {
        val people = TMDb3.People.details(
            personId = id.toInt(),
            appendToResponse = listOfNotNull(
                if (page > 1) null else TMDb3.Params.AppendToResponse.Person.COMBINED_CREDITS,
            ),
        ).let { person ->
            People(
                id = person.id.toString(),
                name = person.name,
                image = person.profilePath?.w500,

                filmography = person.combinedCredits?.cast?.mapNotNull { multi ->
                    when (multi.mediaType) {
                        TMDb3.MultiItem.MediaType.MOVIE -> Movie(
                            id = multi.id.toString(),
                            title = multi.title ?: "",
                            overview = multi.overview,
//                            released = multi.releasedDate,
                            rating = multi.voteAverage.toDouble(),
                            poster = multi.posterPath?.w500,
                            banner = multi.backdropPath?.original,
                        )

                        TMDb3.MultiItem.MediaType.TV -> TvShow(
                            id = multi.id.toString(),
                            title = multi.name ?: "",
                            overview = multi.overview,
//                            released = multi.firstAirDate,
                            rating = multi.voteAverage.toDouble(),
                            poster = multi.posterPath?.w500,
                            banner = multi.backdropPath?.original,
                        )

                        else -> null
                    }
                } ?: listOf()
            )
        }

        return people
    }

    override suspend fun getServers(id: String, videoType: Video.Type): List<Video.Server> {
        val servers = listOf(
            VidsrcToExtractor().server(videoType),
        )

        return servers
    }

    override suspend fun getVideo(server: Video.Server): Video {
        return when {
            server.video != null -> server.video!!
            else -> Extractor.extract(server.src)
        }
    }
}