package com.tanasi.streamflix.models

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.utils.format
import com.tanasi.streamflix.utils.toCalendar

@Entity("tv_shows")
class TvShow(
    @PrimaryKey
    var id: String = "",
    var title: String = "",
    var overview: String? = null,
    released: String? = null,
    var runtime: Int? = null,
    var trailer: String? = null,
    var quality: String? = null,
    var rating: Double? = null,
    var poster: String? = null,
    var banner: String? = null,

    @Ignore
    val seasons: List<Season> = listOf(),
    @Ignore
    val genres: List<Genre> = listOf(),
    @Ignore
    val directors: List<People> = listOf(),
    @Ignore
    val cast: List<People> = listOf(),
    @Ignore
    val recommendations: List<Show> = listOf(),
) : Show, AppAdapter.Item {

    var released = released?.toCalendar()
    override var isFavorite: Boolean = false
    var isWatching: Boolean = true

    val episodeToWatch: Episode?
        get() {
            val episodes = seasons.flatMap { it.episodes }
            val episode = episodes
                .filter { it.watchHistory != null }
                .sortedByDescending { it.watchHistory?.lastEngagementTimeUtcMillis }
                .firstOrNull()
                ?: episodes.indexOfLast { it.isWatched }
                    .takeIf { it != -1 && it + 1 < episodes.size }
                    ?.let { episodes.getOrNull(it + 1) }
                ?: seasons.firstOrNull { it.number != 0 }?.episodes?.firstOrNull()
                ?: episodes.firstOrNull()
            return episode
        }

    fun isSame(tvShow: TvShow): Boolean {
        if (isFavorite != tvShow.isFavorite) return false
        if (isWatching != tvShow.isWatching) return false
        return true
    }

    fun merge(tvShow: TvShow): TvShow {
        this.isFavorite = tvShow.isFavorite
        this.isWatching = tvShow.isWatching
        return this
    }


    @Ignore
    override lateinit var itemType: AppAdapter.Type


    fun copy(
        id: String = this.id,
        title: String = this.title,
        overview: String? = this.overview,
        released: String? = this.released?.format("yyyy-MM-dd"),
        runtime: Int? = this.runtime,
        trailer: String? = this.trailer,
        quality: String? = this.quality,
        rating: Double? = this.rating,
        poster: String? = this.poster,
        banner: String? = this.banner,
        seasons: List<Season> = this.seasons,
        genres: List<Genre> = this.genres,
        directors: List<People> = this.directors,
        cast: List<People> = this.cast,
        recommendations: List<Show> = this.recommendations,
    ) = TvShow(
        id,
        title,
        overview,
        released,
        runtime,
        trailer,
        quality,
        rating,
        poster,
        banner,
        seasons,
        genres,
        directors,
        cast,
        recommendations,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TvShow

        if (id != other.id) return false
        if (title != other.title) return false
        if (overview != other.overview) return false
        if (runtime != other.runtime) return false
        if (trailer != other.trailer) return false
        if (quality != other.quality) return false
        if (rating != other.rating) return false
        if (poster != other.poster) return false
        if (banner != other.banner) return false
        if (seasons != other.seasons) return false
        if (genres != other.genres) return false
        if (directors != other.directors) return false
        if (cast != other.cast) return false
        if (recommendations != other.recommendations) return false
        if (released != other.released) return false
        if (isFavorite != other.isFavorite) return false
        if (isWatching != other.isWatching) return false
        if (!::itemType.isInitialized || !other::itemType.isInitialized) return false
        return itemType == other.itemType
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + (overview?.hashCode() ?: 0)
        result = 31 * result + (runtime ?: 0)
        result = 31 * result + (trailer?.hashCode() ?: 0)
        result = 31 * result + (quality?.hashCode() ?: 0)
        result = 31 * result + (rating?.hashCode() ?: 0)
        result = 31 * result + (poster?.hashCode() ?: 0)
        result = 31 * result + (banner?.hashCode() ?: 0)
        result = 31 * result + seasons.hashCode()
        result = 31 * result + genres.hashCode()
        result = 31 * result + directors.hashCode()
        result = 31 * result + cast.hashCode()
        result = 31 * result + recommendations.hashCode()
        result = 31 * result + (released?.hashCode() ?: 0)
        result = 31 * result + isFavorite.hashCode()
        result = 31 * result + isWatching.hashCode()
        result = 31 * result + (if (::itemType.isInitialized) itemType.hashCode() else 0)
        return result
    }
}