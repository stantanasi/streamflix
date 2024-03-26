package com.tanasi.streamflix.utils

import com.google.gson.annotations.SerializedName
import com.tanasi.streamflix.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object TMDb3 {

    private const val URL = "https://api.themoviedb.org/3/"
    private const val API_KEY = BuildConfig.TMDB_API_KEY

    private val service = ApiService.build()


    private interface ApiService {

        companion object {
            fun build(): ApiService {
                val client = OkHttpClient.Builder().addInterceptor { chain ->
                    val original = chain.request()

                    val requestBuilder = original.newBuilder()
                        .url(
                            original.url.newBuilder()
                                .addQueryParameter("api_key", API_KEY)
                                .build()
                        )

                    chain.proceed(requestBuilder.build())
                }.build()

                val retrofit = Retrofit.Builder()
                    .baseUrl(URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                return retrofit.create(ApiService::class.java)
            }
        }
    }


    data class Result<T>(
        val results: List<T>
    )

    data class PageResult<T>(
        @SerializedName("page") val page: Int,
        @SerializedName("results") val results: List<T> = emptyList(),
        @SerializedName("total_pages") val totalPages: Int,
        @SerializedName("total_results") val totalResults: Int,
    )

    data class GenresResponse(
        val genres: List<Genre>,
    )

    data class MultiItem(
        @SerializedName("poster_path") val posterPath: String?,
        @SerializedName("adult") val adult: Boolean = false,
        @SerializedName("overview") val overview: String,
        @SerializedName("genre_ids") val genresIds: List<Int>,
        @SerializedName("id") val id: Int,
        @SerializedName("original_language") val originalLanguage: String,
        @SerializedName("title") val title: String? = null,
        @SerializedName("backdrop_path") val backdropPath: String?,
        @SerializedName("popularity") val popularity: Float,
        @SerializedName("vote_count") val voteCount: Int,
        @SerializedName("vote_average") val voteAverage: Float,
        @SerializedName("name") val name: String? = null,
        @SerializedName("media_type") val mediaType: MediaType,
    ) {

        enum class MediaType {
            @SerializedName("movie")
            MOVIE,

            @SerializedName("person")
            PERSON,

            @SerializedName("tv")
            TV,
        }
    }

    data class WatchProviderResult(
        val id: Int? = null,
        val results: Map<String, Providers>
    )

    data class Genre(
        val id: Int,
        val name: String,
    ) {

        enum class Movie(val id: Int) {
            ACTION(28),
            ADVENTURE(12),
            ANIMATION(16),
            COMEDY(35),
            CRIME(80),
            DOCUMENTARY(99),
            DRAMA(18),
            FAMILY(10751),
            FANTASY(14),
            HISTORY(36),
            HORROR(27),
            MUSIC(10402),
            MYSTERY(9648),
            ROMANCE(10749),
            SCIENCE_FICTION(878),
            TV_MOVIE(10770),
            THRILLER(53),
            WAR(10752),
            WESTERN(37);

            override fun toString() = id.toString()
        }

        enum class Tv(val id: Int) {
            ACTION_ADVENTURE(10759),
            ANIMATION(16),
            COMEDY(35),
            CRIME(80),
            DOCUMENTARY(99),
            DRAMA(18),
            FAMILY(10751),
            KIDS(10762),
            MYSTERY(9648),
            NEWS(10763),
            REALITY(10764),
            SCIENCE_FICTION_FANTASY(10765),
            SOAP(10766),
            TALK(10767),
            WAR_POLITICS(10768),
            WESTERN(37);

            override fun toString() = id.toString()
        }
    }

    data class Movie(
        @SerializedName("poster_path") val posterPath: String?,
        @SerializedName("adult") val adult: Boolean = false,
        @SerializedName("overview") val overview: String,
        @SerializedName("release_date") val releaseDate: String? = null,
        @SerializedName("genre_ids") val genresIds: List<Int>,
        @SerializedName("id") val id: Int,
        @SerializedName("original_title") val originalTitle: String,
        @SerializedName("original_language") val originalLanguage: String,
        @SerializedName("title") val title: String,
        @SerializedName("backdrop_path") val backdropPath: String?,
        @SerializedName("popularity") val popularity: Float,
        @SerializedName("vote_count") val voteCount: Int,
        @SerializedName("video") val video: Boolean,
        @SerializedName("vote_average") val voteAverage: Float,
    ) {

        enum class ReleaseType(val value: Int) {
            @SerializedName("1")
            PREMIERE(1),

            @SerializedName("2")
            THEATRICAL_LIMITED(2),

            @SerializedName("3")
            THEATRICAL(3),

            @SerializedName("4")
            DIGITAL(4),

            @SerializedName("5")
            PHYSICAL(5),

            @SerializedName("6")
            TV(6),
        }

        enum class Status(val value: String) {
            @SerializedName("Rumored")
            RUMORED("Rumored"),

            @SerializedName("Planned")
            PLANNED("Planned"),

            @SerializedName("In Production")
            IN_PRODUCTION("In Production"),

            @SerializedName("Post Production")
            POST_PRODUCTION("Post Production"),

            @SerializedName("Released")
            RELEASED("Released"),

            @SerializedName("Canceled")
            CANCELED("Canceled"),
        }

        data class Detail(
            val adult: Boolean,
            @SerializedName("backdrop_path") val backdropPath: String?,
            val budget: Long,
            @SerializedName("genres") val genres: List<Genre>,
            val homepage: String? = null,
            val id: Int,
            @SerializedName("imdb_id") val imdbId: String? = null,
            val title: String,
            val runtime: Int? = null,
            @SerializedName("original_title") val originalTitle: String,
            @SerializedName("original_language") val originalLanguage: String,
            val overview: String,
            @SerializedName("poster_path") val posterPath: String?,
            @SerializedName("vote_average") val voteAverage: Float,
            @SerializedName("vote_count") val voteCount: Int,
            @SerializedName("external_ids") val externalIds: ExternalIds? = null,
            val status: Status,
            val tagline: String,
            val video: Boolean,
            val popularity: Float,
            @SerializedName("release_date") val releaseDate: String?,
            val revenue: Long,
            @SerializedName("release_dates") val releaseDates: Result<ReleaseDates>? = null,
            @SerializedName("production_companies") val productionCompanies: List<Company>? = null,
            @SerializedName("production_countries") val productionCountries: List<Country>? = null,
            @SerializedName("watch/providers") val watchProviders: WatchProviderResult? = null,
            @SerializedName("credits") val credits: Credits? = null,
            @SerializedName("videos") val videos: Result<Video>? = null,
            @SerializedName("images") val images: Images? = null,
            @SerializedName("recommendations") val recommendations: PageResult<MultiItem>? = null,
        )

        data class Country(
            @SerializedName("iso_3166_1") val iso3166: String,
            val name: String
        )

        data class ReleaseDates(
            @SerializedName("iso_3166_1") val iso3166: String,
            @SerializedName("release_dates") val releaseDates: List<ReleaseDate>
        )

        data class ReleaseDate(
            @SerializedName("iso_639_1") val iso639: String? = null,
            @SerializedName("release_date") val releaseDate: String?,
            val certification: String? = null,
            val type: ReleaseType,
        )
    }

    data class Tv(
        @SerializedName("poster_path") val posterPath: String?,
        @SerializedName("popularity") val popularity: Float,
        @SerializedName("id") val id: Int,
        @SerializedName("adult") val adult: Boolean = false,
        @SerializedName("backdrop_path") val backdropPath: String?,
        @SerializedName("vote_average") val voteAverage: Float,
        @SerializedName("overview") val overview: String,
        @SerializedName("first_air_date") val firstAirDate: String? = null,
        @SerializedName("origin_country") val originCountry: List<String>,
        @SerializedName("genre_ids") val genresIds: List<Int>,
        @SerializedName("original_language") val originalLanguage: String,
        @SerializedName("vote_count") val voteCount: Int,
        @SerializedName("name") val name: String,
        @SerializedName("original_name") val originalName: String,
    ) {

        enum class Status(val value: String, val id: Int) {
            @SerializedName("Returning Series")
            RETURNING_SERIES("Returning Series", 0),

            @SerializedName("In Production")
            IN_PRODUCTION("In Production", 2),

            @SerializedName("Planned")
            PLANNED("Planned", 1),

            @SerializedName("Canceled")
            CANCELED("Canceled", 4),

            @SerializedName("Ended")
            ENDED("Ended", 3),

            @SerializedName("Pilot")
            PILOT("Pilot", 5),
        }

        enum class Type(val value: String) {
            @SerializedName("Scripted")
            SCRIPTED("Scripted"),

            @SerializedName("Reality")
            REALITY("Reality"),

            @SerializedName("Documentary")
            DOCUMENTARY("Documentary"),

            @SerializedName("News")
            NEWS("News"),

            @SerializedName("Talk")
            TALK("Talk"),

            @SerializedName("Talk Show")
            TALK_SHOW("Talk Show"),

            @SerializedName("Show")
            SHOW("Show"),

            @SerializedName("Miniseries")
            MINISERIES("Miniseries"),

            @SerializedName("Video")
            VIDEO("Video"),
        }

        data class Detail(
            @SerializedName("id") val id: Int,
            val name: String,
            @SerializedName("poster_path") val posterPath: String?,
            @SerializedName("backdrop_path") val backdropPath: String?,
            val popularity: Float,
            @SerializedName("first_air_date") val firstAirDate: String? = null,
            @SerializedName("last_air_date") val lastAirDate: String? = null,
            @SerializedName("genres") val genres: List<Genre>,
            @SerializedName("last_episode_to_air") val lastEpisodeToAir: Episode? = null,
            @SerializedName("next_episode_to_air") val nextEpisodeToAir: Episode? = null,
            @SerializedName("number_of_episodes") val numberOfEpisodes: Int,
            @SerializedName("number_of_seasons") val numberOfSeasons: Int,
            @SerializedName("episode_run_time") val episodeRuntime: List<Int>,
            @SerializedName("production_companies") val productionCompanies: List<Company>? = null,
            val homepage: String? = null,
            @SerializedName("in_production") val inProduction: Boolean,
            val seasons: List<Season>,
            val networks: List<Network> = emptyList(),
            val status: Status? = null,
            val type: Type? = null,
            val languages: List<String>,
            @SerializedName("origin_country") val originCountry: List<String>,
            @SerializedName("original_language") val originalLanguage: String,
            @SerializedName("original_name") val originalName: String,
            val overview: String,
            val tagline: String,
            @SerializedName("vote_average") val voteAverage: Float,
            @SerializedName("vote_count") val voteCount: Int,
            @SerializedName("external_ids") val externalIds: ExternalIds? = null,
            @SerializedName("watch/providers") val watchProviders: WatchProviderResult? = null,
            @SerializedName("credits") val credits: Credits? = null,
            @SerializedName("aggregate_credits") val aggregateCredits: Credits.AggregateCredits? = null,
            @SerializedName("videos") val videos: Result<Video>? = null,
            @SerializedName("content_ratings") val contentRatings: Result<ContentRating>? = null,
            @SerializedName("images") val images: Images? = null,
            @SerializedName("created_by") val createdBy: List<CreatedBy>? = null,
            @SerializedName("recommendations") val recommendations: PageResult<MultiItem>? = null,
        )

        data class ContentRating(
            @SerializedName("iso_3166_1") val iso3166: String,
            @SerializedName("rating") val rating: String,
        )

        data class CreatedBy(
            @SerializedName("id") val id: Int,
            @SerializedName("credit_id") val creditId: String? = null,
            @SerializedName("gender") val gender: Person.Gender? = null,
            @SerializedName("name") val name: String,
            @SerializedName("profile_path") val profilePath: String? = null,
        )
    }

    data class Season(
        @SerializedName("id") val id: Int,
        @SerializedName("air_date") val airDate: String? = null,
        @SerializedName("episode_count") val episodeCount: Int? = null,
        @SerializedName("name") val name: String,
        @SerializedName("poster_path") val posterPath: String?,
        @SerializedName("season_number") val seasonNumber: Int,
        @SerializedName("overview") val overview: String? = null,
        @SerializedName("episodes") val episodes: List<Episode>? = null,
    ) {

        data class Detail(
            @SerializedName("id") val id: Int,
            @SerializedName("air_date") val airDate: String? = null,
            @SerializedName("episode_count") val episodeCount: Int? = null,
            @SerializedName("name") val name: String,
            @SerializedName("poster_path") val posterPath: String?,
            @SerializedName("season_number") val seasonNumber: Int,
            @SerializedName("overview") val overview: String,
            @SerializedName("vote_average") val voteAverage: Float? = null,
            @SerializedName("episodes") val episodes: List<Episode>? = null,
            @SerializedName("external_ids") val externalIds: ExternalIds? = null,
            @SerializedName("videos") val videos: Result<Video>? = null,
            @SerializedName("images") val images: Images? = null,
        )
    }

    data class Episode(
        @SerializedName("id") val id: Int,
        @SerializedName("overview") val overview: String? = null,
        @SerializedName("episode_number") val episodeNumber: Int,
        @SerializedName("season_number") val seasonNumber: Int,
        @SerializedName("air_date") val airDate: String? = null,
        @SerializedName("name") val name: String? = null,
        @SerializedName("vote_average") val voteAverage: Float? = null,
        @SerializedName("vote_count") val voteCount: Int? = null,
        @SerializedName("still_path") val stillPath: String? = null,
        @SerializedName("crew") val crew: List<Crew>? = null,
        @SerializedName("guest_stars") val guestStars: List<Cast>? = null,
    ) {

        data class Detail(
            @SerializedName("id") val id: Int,
            @SerializedName("overview") val overview: String,
            @SerializedName("episode_number") val episodeNumber: Int,
            @SerializedName("season_number") val seasonNumber: Int,
            @SerializedName("air_date") val airDate: String? = null,
            @SerializedName("name") val name: String? = null,
            @SerializedName("vote_average") val voteAverage: Float? = null,
            @SerializedName("vote_count") val voteCount: Int? = null,
            @SerializedName("still_path") val stillPath: String? = null,
            @SerializedName("images") val images: Result<Images>? = null,
            @SerializedName("crew") val crew: List<Crew>? = null,
            @SerializedName("guest_stars") val guestStars: List<Cast>? = null,
            @SerializedName("external_ids") val externalIds: ExternalIds? = null
        )
    }

    data class Person(
        @SerializedName("adult") val adult: Boolean,
        @SerializedName("gender") val gender: Gender,
        @SerializedName("id") val id: Int,
        @SerializedName("known_for_department") val knownForDepartment: Department? = null,
        @SerializedName("name") val name: String,
        @SerializedName("profile_path") val profilePath: String? = null,
        @SerializedName("popularity") val popularity: Float
    ) {

        enum class Gender(val value: Int) {
            @SerializedName("0")
            UNKNOWN(0),

            @SerializedName("1")
            FEMALE(1),

            @SerializedName("2")
            MALE(2),

            @SerializedName("3")
            NON_BINARY(3),
        }

        enum class Department(val value: String) {
            @SerializedName("Acting")
            ACTING("Acting"),

            @SerializedName("Writing")
            WRITING("Writing"),

            @SerializedName("Sound")
            SOUND("Sound"),

            @SerializedName("Production")
            PRODUCTION("Production"),

            @SerializedName("Art")
            ART("Art"),

            @SerializedName("Directing")
            DIRECTING("Directing"),

            @SerializedName("Creator")
            CREATOR("Creator"),

            @SerializedName("Costume & Make-Up")
            COSTUME_AND_MAKEUP("Costume & Make-Up"),

            @SerializedName("Camera")
            CAMERA("Camera"),

            @SerializedName("Visual Effects")
            VISUAL_EFFECTS("Visual Effects"),

            @SerializedName("Lighting")
            LIGHTING("Lighting"),

            @SerializedName("Editing")
            EDITING("Editing"),

            @SerializedName("Actors")
            ACTORS("Actors"),

            @SerializedName("Crew")
            CREW("Crew"),
        }

        data class Detail(
            @SerializedName("adult") val adult: Boolean,
            @SerializedName("also_known_as") val alsoKnownAs: List<String>,
            @SerializedName("biography") val biography: String? = null,
            @SerializedName("birthday") val birthday: String? = null,
            @SerializedName("deathday") val deathday: String? = null,
            @SerializedName("gender") val gender: Gender? = null,
            @SerializedName("homepage") val homepage: String? = null,
            @SerializedName("id") val id: Int,
            @SerializedName("imdb_id") val imdbId: String? = null,
            @SerializedName("known_for_department") val knownForDepartment: Department? = null,
            @SerializedName("name") val name: String,
            @SerializedName("place_of_birth") val placeOfBirth: String? = null,
            @SerializedName("popularity") val popularity: Float? = null,
            @SerializedName("profile_path") val profilePath: String? = null,
            @SerializedName("external_ids") val externalIds: ExternalIds? = null,
            @SerializedName("images") val images: Images? = null,
//            @SerializedName("tagged_images") val taggedImages: TmdbImagePageResult? = null,
            @SerializedName("combined_credits") val combinedCredits: Credits<MultiItem>? = null,
            @SerializedName("movie_credits") val movieCredits: Credits<Credit.Movie>? = null,
            @SerializedName("tv_credits") val tvCredits: Credits<Credit.Tv>? = null,
//            @SerializedName("translations") val translations: TmdbPersonTranslations? = null,
        )

        data class Role(
            @SerializedName("credit_id") val creditId: String,
            @SerializedName("character") val character: String,
            @SerializedName("episode_count") val episodeCount: Int,
        )

        data class Job(
            @SerializedName("credit_id") val creditId: String,
            @SerializedName("job") val job: String,
            @SerializedName("episode_count") val episodeCount: Int,
        )

        data class Credits<T>(
            @SerializedName("cast") val cast: List<T>,
            @SerializedName("crew") val crew: List<T>,
        )

        sealed interface Credit {

            data class Movie(
                @SerializedName("poster_path") val posterPath: String?,
                @SerializedName("adult") val adult: Boolean = false,
                @SerializedName("overview") val overview: String,
                @SerializedName("release_date") val releaseDate: String? = null,
                @SerializedName("genre_ids") val genreIds: List<Int>,
                @SerializedName("id") val id: Int,
                @SerializedName("original_title") val originalTitle: String? = null,
                @SerializedName("original_language") val originalLanguage: String,
                @SerializedName("title") val title: String? = null,
                @SerializedName("backdrop_path") val backdropPath: String?,
                @SerializedName("popularity") val popularity: Float,
                @SerializedName("video") val video: Boolean = false,
                @SerializedName("vote_average") val voteAverage: Float,
                @SerializedName("vote_count") val voteCount: Int,
                @SerializedName("character") val character: String? = null,
                @SerializedName("credit_id") val creditId: String? = null,
                @SerializedName("order") val order: Int? = null,
                @SerializedName("department") val department: Department? = null,
                @SerializedName("job") val job: String? = null,
            ) : Credit

            data class Tv(
                @SerializedName("poster_path") val posterPath: String? = null,
                @SerializedName("popularity") val popularity: Float? = null,
                @SerializedName("id") val id: Int,
                @SerializedName("adult") val adult: Boolean = false,
                @SerializedName("backdrop_path") val backdropPath: String? = null,
                @SerializedName("vote_average") val voteAverage: Float,
                @SerializedName("overview") val overview: String,
                @SerializedName("first_air_date") val firstAirDate: String? = null,
                @SerializedName("origin_country") val originCountry: List<String> = emptyList(),
                @SerializedName("genre_ids") val genreIds: List<Int> = emptyList(),
                @SerializedName("original_language") val originalLanguage: String,
                @SerializedName("vote_count") val voteCount: Int,
                @SerializedName("name") val name: String? = null,
                @SerializedName("original_name") val originalName: String? = null,
                @SerializedName("character") val character: String? = null,
                @SerializedName("credit_id") val creditId: String? = null,
                @SerializedName("order") val order: Int? = null,
                @SerializedName("department") val department: Department? = null,
                @SerializedName("job") val job: String? = null,
            ) : Credit
        }
    }

    data class Credits(
        @SerializedName("cast") val cast: List<Cast>,
        @SerializedName("crew") val crew: List<Crew>,
    ) {

        data class AggregateCredits(
            @SerializedName("cast") val cast: List<Cast.AggregateCast>,
            @SerializedName("crew") val crew: List<Crew.AggregateCrew>,
        )
    }

    data class Crew(
        @SerializedName("adult") val adult: Boolean = false,
        @SerializedName("gender") val gender: Person.Gender = Person.Gender.UNKNOWN,
        @SerializedName("id") val id: Int,
        @SerializedName("known_for_department") val knownForDepartment: Person.Department? = null,
        @SerializedName("name") val name: String,
        @SerializedName("original_name") val originalName: String? = null,
        @SerializedName("popularity") val popularity: Float? = null,
        @SerializedName("profile_path") val profilePath: String? = null,
        @SerializedName("credit_id") val creditId: String,
        @SerializedName("department") val department: Person.Department? = null,
        @SerializedName("job") val job: String,
    ) {

        data class AggregateCrew(
            @SerializedName("adult") val adult: Boolean = false,
            @SerializedName("gender") val gender: Person.Gender,
            @SerializedName("id") val id: Int,
            @SerializedName("known_for_department") val knownForDepartment: Person.Department? = null,
            @SerializedName("name") val name: String,
            @SerializedName("original_name") val originalName: String? = null,
            @SerializedName("popularity") val popularity: Float? = null,
            @SerializedName("profile_path") val profilePath: String? = null,
            @SerializedName("jobs") val jobs: List<Person.Job>,
            @SerializedName("department") val department: Person.Department? = null,
            @SerializedName("total_episode_count") val totalEpisodeCount: Int,
        )
    }

    data class Cast(
        @SerializedName("adult") val adult: Boolean = false,
        @SerializedName("gender") val gender: Person.Gender,
        @SerializedName("id") val id: Int,
        @SerializedName("known_for_department") val knownForDepartment: Person.Department? = null,
        @SerializedName("name") val name: String,
        @SerializedName("original_name") val originalName: String? = null,
        @SerializedName("popularity") val popularity: Float? = null,
        @SerializedName("profile_path") val profilePath: String? = null,
        @SerializedName("cast_id") val castId: Int? = null,
        @SerializedName("character") val character: String,
        @SerializedName("credit_id") val creditId: String,
        @SerializedName("order") val order: Int,
    ) {

        data class AggregateCast(
            @SerializedName("adult") val adult: Boolean = false,
            @SerializedName("gender") val gender: Person.Gender,
            @SerializedName("id") val id: Int,
            @SerializedName("known_for_department") val knownForDepartment: Person.Department? = null,
            @SerializedName("name") val name: String,
            @SerializedName("original_name") val originalName: String? = null,
            @SerializedName("popularity") val popularity: Float? = null,
            @SerializedName("profile_path") val profilePath: String? = null,
            @SerializedName("roles") val roles: List<Person.Role>,
            @SerializedName("total_episode_count") val totalEpisodeCount: Int,
            @SerializedName("order") val order: Int,
        )
    }

    data class Company(
        @SerializedName("id") val id: Int,
        @SerializedName("logo_path") val logoPath: String? = null,
        @SerializedName("name") val name: String? = null,
        @SerializedName("origin_country") val originCountry: String? = null
    ) {

        enum class CompanyId(val id: Int) {
            CENTURY_STUDIOS(127928),
            COLUMBIA_PICTURES(5),
            NETFLIX_INTERNATIONAL_PICTURES(145174),
            NEW_LINE_CINEMA(12),
            PARAMOUNT_PICTURES(107355),
            TRISTAR_PICTURES(559),
            UNIVERSAL(33),
            WALT_DISNEY_PICTURES(2),
            WARNER_BROS_PICTURES(174);

            override fun toString() = id.toString()
        }
    }

    data class ExternalIds(
        @SerializedName("imdb_id") val imdbId: String? = null,
        @SerializedName("freebase_mid") val freebaseMid: String? = null,
        @SerializedName("freebase_id") val freebaseId: String? = null,
        @SerializedName("tvdb_id") val tvdbId: Int? = null,
        @SerializedName("tvrage_id") val tvrageId: Int? = null,
        @SerializedName("id") val id: Int? = null, // it is is used in append responses
        @SerializedName("facebook_id") val facebook: String? = null,
        @SerializedName("instagram_id") val instagram: String? = null,
        @SerializedName("tiktok_id") val tiktok: String? = null,
        @SerializedName("twitter_id") val twitter: String? = null,
        @SerializedName("wikidata_id") val wikidata: String? = null,
        @SerializedName("youtube_id") val youtube: String? = null,
    )

    data class Images(
        @SerializedName("id") val id: Int? = null,
        @SerializedName("posters") val posters: List<FileImage> = emptyList(),
        @SerializedName("backdrops") val backdrops: List<FileImage> = emptyList()
    ) {

        data class FileImage(
            @SerializedName("file_path") val filePath: String,
            @SerializedName("aspect_ratio") val aspectRation: Float,
            @SerializedName("height") val height: Int,
            @SerializedName("width") val width: Int,
            @SerializedName("iso_639_1") val iso639: String? = null,
            @SerializedName("vote_average") val voteAverage: Float? = null,
            @SerializedName("vote_count") val voteCount: Int? = null
        )
    }

    data class Providers(
        val link: String,
        val flatrate: List<Provider> = emptyList(),
        val buy: List<Provider> = emptyList()
    )

    data class Network(
        @SerializedName("id") val id: Int,
        @SerializedName("name") val name: String? = null,
        @SerializedName("origin_country") val originCountry: String? = null,
        @SerializedName("headquarters") val headquarters: String? = null,
        @SerializedName("homepage") val homepage: String? = null,
        @SerializedName("images") val images: NetworkImages? = null,
        @SerializedName("logo_path") val logoPath: String? = null
    ) {

        enum class NetworkId(val id: Int) {
            ABC(2),
            ADULT_SWIM(80),
            AMAZON(1024),
            AMC(174),
            ANIMAL_PLANET(91),
            APPLE_TV(2552),
            A_AND_E(129),
            BBC_AMERICA(493),
            BRAVO(74),
            CBS(16),
            COMEDY_CENTRAL(47),
            DISNEY_PLUS(2739),
            ESPN(29),
            FOX(19),
            FUJI_TV(2),
            FX(88),
            HBO(49),
            HISTORY(65),
            HULU(453),
            LIFETIME(34),
            NATIONAL_GEOGRAPHIC(43),
            NBC(6),
            NETFLIX(213),
            NICKELODEON(13),
            PARAMOUNT_PLUS(4330),
            PBS(14),
            PEACOCK(3353),
            SHOWTIME(67),
            SYFY(77),
            THE_CW(71),
            TNT(41),
            USA(30),
            VH1(158),
            YOUTUBE_PREMIUM(1436);

            override fun toString() = id.toString()
        }

        data class NetworkImages(
            @SerializedName("logos") val logos: List<LogoImage> = emptyList()
        )

        data class LogoImage(
            @SerializedName("file_path") val filePath: String?
        )
    }

    data class Provider(
        @SerializedName("display_priority") val displayPriority: Int?,
        @SerializedName("logo_path") val logoPath: String,
        @SerializedName("provider_id") val providerId: Int,
        @SerializedName("provider_name") val providerName: String
    ) {

        enum class WatchProviderId(val id: Int) {
            AMAZON_PRIME_VIDEO_TIER_A(9),
            AMAZON_PRIME_VIDEO_TIER_B(119),
            AMAZON_VIDEO(10),
            APPLE_ITUNES(2),
            APPLE_TV_PLUS(350),
            DISNEY_PLUS(337),
            GOOGLE_PLAY(3),
            HBO_MAX(384),
            HULU(15),
            MICROSOFT_STORE(68),
            NETFLIX(8),
            PARAMOUNT(531);

            override fun toString() = id.toString()
        }

        enum class WatchMonetizationType(val value: String) {
            @SerializedName("flatrate")
            FLATRATE("flatrate"),

            @SerializedName("free")
            FREE("free"),

            @SerializedName("ads")
            ADS("ads"),

            @SerializedName("rent")
            RENT("rent"),

            @SerializedName("buy")
            BUY("buy")
        }
    }

    data class Keyword(
        val id: Int,
        val name: String,
    ) {

        enum class KeywordId(val id: Int) {
            ANIME(210024),
            BASED_ON_ANIME(222243);

            override fun toString() = id.toString()
        }
    }

    data class Video(
        @SerializedName("id") val id: String,
        @SerializedName("iso_639_1") val iso639: String? = null,
        @SerializedName("iso_3166_1") val iso3166: String? = null,
        @SerializedName("key") val key: String? = null,
        @SerializedName("site") val site: VideoSite? = null,
        @SerializedName("name") val name: String? = null,
        @SerializedName("size") val size: Int? = null, // 360, 480, 720, 1080
        @SerializedName("type") val type: VideoType? = null,
        @SerializedName("published_at") val publishedAt: String? = null,
    ) {

        enum class VideoSite(val value: String) {
            @SerializedName("YouTube")
            YOUTUBE("YouTube"),

            @SerializedName("Vimeo")
            VIMEO("Vimeo"),
        }

        enum class VideoType(val value: String) {
            @SerializedName("Trailer")
            TRAILER("Trailer"),

            @SerializedName("Teaser")
            TEASER("Teaser"),

            @SerializedName("Clip")
            CLIP("Clip"),

            @SerializedName("Featurette")
            FEATURETTE("Featurette"),

            @SerializedName("Bloopers")
            BLOOPERS("Bloopers"),

            @SerializedName("Opening Credits")
            OPENING_CREDITS("Opening Credits"),

            @SerializedName("Behind the Scenes")
            BEHIND_THE_SCENES("Behind the Scenes"),
        }
    }
}
