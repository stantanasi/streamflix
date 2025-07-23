package com.tanasi.streamflix.models.cuevanaeu

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse(
    @SerialName("props") var props: Props? = Props(),
)

@Serializable
data class Props(
    @SerialName("pageProps") var pageProps: PageProps? = PageProps(),
)

@Serializable
data class PageProps(
    @SerialName("movies") var movies: ArrayList<MediaItem>? = arrayListOf(),
    @SerialName("thisMovie") var thisMovie: MediaItem? = MediaItem(),
    @SerialName("thisSerie") var thisSerie: MediaItem? = MediaItem(),
    @SerialName("episode") var episode: EpisodeInfo? = EpisodeInfo()
)

@Serializable
data class MediaItem(
    @SerialName("titles") var titles: Titles? = Titles(),
    @SerialName("images") var images: Images? = Images(),
    @SerialName("overview") var overview: String? = null,
    @SerialName("releaseDate") var releaseDate: String? = null,
    @SerialName("genres") var genres: ArrayList<GenreInfo>? = arrayListOf(),
    @SerialName("cast") var cast: Cast? = Cast(),
    @SerialName("videos") var videos: Videos? = Videos(),
    @SerialName("seasons") var seasons: ArrayList<SeasonInfo>? = arrayListOf(),
    @SerialName("slug") var slug: Slug? = Slug(),
    @SerialName("url") var url: Url? = Url(),
    @SerialName("rate") var rate: Rate? = Rate(),
)

@Serializable
data class SeasonInfo(
    @SerialName("number") var number: Int? = null,
    @SerialName("episodes") var episodes: ArrayList<EpisodeItem> = arrayListOf(),
)

@Serializable
data class EpisodeItem(
    @SerialName("title") var title: String? = null,
    @SerialName("number") var number: Int? = null,
    @SerialName("releaseDate") var releaseDate: String? = null,
    @SerialName("image") var image: String? = null,
    @SerialName("slug") var slug: Slug? = Slug(),
)

@Serializable
data class EpisodeInfo(
    @SerialName("videos") var videos: Videos? = Videos()
)

@Serializable
data class VideoInfo(
    @SerialName("cyberlocker") var cyberlocker: String? = null,
    @SerialName("result") var result: String? = null,
)

@Serializable
data class Videos(
    @SerialName("latino") var latino: ArrayList<VideoInfo>? = arrayListOf(),
    @SerialName("spanish") var spanish: ArrayList<VideoInfo>? = arrayListOf(),
    @SerialName("english") var english: ArrayList<VideoInfo>? = arrayListOf(),
    @SerialName("japanese") var japanese: ArrayList<VideoInfo>? = arrayListOf()
)

@Serializable
data class Titles(
    @SerialName("name") var name: String? = null,
)

@Serializable
data class Images(
    @SerialName("poster") var poster: String? = null,
    @SerialName("backdrop") var backdrop: String? = null,
)

@Serializable
data class GenreInfo(
    @SerialName("name") var name: String? = null,
    @SerialName("slug") var slug: String? = null
)

@Serializable
data class Cast(
    @SerialName("acting") var acting: ArrayList<ActorInfo>? = arrayListOf()
)

@Serializable
data class ActorInfo(
    @SerialName("name") var name: String? = null
)

@Serializable
data class Url(
    @SerialName("slug") var slug: String? = null,
)

@Serializable
data class Slug(
    @SerialName("name") var name: String? = null,
    @SerialName("season") var season: String? = null,
    @SerialName("episode") var episode: String? = null,
)

@Serializable
data class Rate(
    @SerialName("average") var average: Double? = null,
)