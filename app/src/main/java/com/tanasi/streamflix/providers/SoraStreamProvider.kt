package com.tanasi.streamflix.providers

import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.models.Category
import com.tanasi.streamflix.models.Episode
import com.tanasi.streamflix.models.Genre
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.models.People
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.models.Video
import com.tanasi.streamflix.utils.TMDb3
import com.tanasi.streamflix.utils.TMDb3.original
import com.tanasi.streamflix.utils.TMDb3.w500
import com.tanasi.streamflix.utils.safeSubList
import java.util.Calendar

object SoraStreamProvider : Provider {

    override val name = "SoraStream"
    override val logo = ""
    override val url = ""

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
}