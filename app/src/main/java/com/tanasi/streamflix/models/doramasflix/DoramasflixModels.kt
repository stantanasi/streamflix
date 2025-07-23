package com.tanasi.streamflix.models.doramasflix

import com.google.gson.annotations.SerializedName

data class ApiResponse(
    val data: Data? = null,
)

data class Data(
    val paginationDorama: Pagination? = null,
    val paginationMovie: Pagination? = null,
    val searchDorama: List<Show>? = null,
    val searchMovie: List<Show>? = null,
    val listSeasons: List<Season>? = null,
    val listEpisodes: List<Episode>? = null,
)

data class Pagination(
    val items: List<Show> = emptyList(),
    val pageInfo: PageInfo? = null,
)

data class Show(
    @SerializedName("_id")
    val id: String,
    val name: String,
    @SerializedName("name_es")
    val nameEs: String? = null,
    val slug: String,
    val overview: String? = null,
    @SerializedName("poster_path")
    val posterPath: String? = null,
    val poster: String? = null,
    val genres: List<Genre> = emptyList(),
    @SerializedName("__typename")
    val typename: String,
)

data class Genre(
    val name: String? = null,
    val slug: String? = null,
)

data class PageInfo(
    val hasNextPage: Boolean? = false,
)

data class Season(
    val slug: String,
    @SerializedName("season_number")
    val seasonNumber: Int,
    @SerializedName("poster_path")
    val posterPath: String? = null,
)

data class Episode(
    @SerializedName("_id")
    val id: String,
    val name: String?,
    val slug: String,
    @SerializedName("episode_number")
    val episodeNumber: Int?,
    @SerializedName("season_number")
    val seasonNumber: Int?,
    @SerializedName("still_path")
    val stillPath: String? = null,
)

// --- MODELOS PARA EL TOKEN DE FKPLAYER ---
data class TokenModel(
    val props: PropsToken? = null,
)

data class PropsToken(
    val pageProps: PagePropsToken? = null,
)

data class PagePropsToken(
    val token: String? = null,
)

data class VideoToken(
    val link: String? = null,
)