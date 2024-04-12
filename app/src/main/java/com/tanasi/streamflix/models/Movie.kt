package com.tanasi.streamflix.models

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.utils.format
import com.tanasi.streamflix.utils.toCalendar
import java.util.Calendar

@Entity("movies")
class Movie(
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
    val genres: List<Genre> = listOf(),
    @Ignore
    val directors: List<People> = listOf(),
    @Ignore
    val cast: List<People> = listOf(),
    @Ignore
    val recommendations: List<Show> = listOf(),
) : Show, WatchItem, AppAdapter.Item {

    var released = released?.toCalendar()
    override var isFavorite: Boolean = false
    override var isWatched: Boolean = false
    override var watchedDate: Calendar? = null

    @Embedded
    override var watchHistory: WatchItem.WatchHistory? = null


    fun isSame(movie: Movie): Boolean {
        if (isFavorite != movie.isFavorite) return false
        if (isWatched != movie.isWatched) return false
        if (watchedDate != movie.watchedDate) return false
        if (watchHistory != movie.watchHistory) return false
        return true
    }

    fun merge(movie: Movie): Movie {
        this.isFavorite = movie.isFavorite
        this.isWatched = movie.isWatched
        this.watchedDate = movie.watchedDate
        this.watchHistory = movie.watchHistory
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
        genres: List<Genre> = this.genres,
        directors: List<People> = this.directors,
        cast: List<People> = this.cast,
        recommendations: List<Show> = this.recommendations,
    ) = Movie(
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
        genres,
        directors,
        cast,
        recommendations,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Movie

        if (id != other.id) return false
        if (title != other.title) return false
        if (overview != other.overview) return false
        if (runtime != other.runtime) return false
        if (trailer != other.trailer) return false
        if (quality != other.quality) return false
        if (rating != other.rating) return false
        if (poster != other.poster) return false
        if (banner != other.banner) return false
        if (genres != other.genres) return false
        if (directors != other.directors) return false
        if (cast != other.cast) return false
        if (recommendations != other.recommendations) return false
        if (released != other.released) return false
        if (isFavorite != other.isFavorite) return false
        if (isWatched != other.isWatched) return false
        if (watchedDate != other.watchedDate) return false
        if (watchHistory != other.watchHistory) return false
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
        result = 31 * result + genres.hashCode()
        result = 31 * result + directors.hashCode()
        result = 31 * result + cast.hashCode()
        result = 31 * result + recommendations.hashCode()
        result = 31 * result + (released?.hashCode() ?: 0)
        result = 31 * result + isFavorite.hashCode()
        result = 31 * result + isWatched.hashCode()
        result = 31 * result + (watchedDate?.hashCode() ?: 0)
        result = 31 * result + (watchHistory?.hashCode() ?: 0)
        result = 31 * result + (if (::itemType.isInitialized) itemType.hashCode() else 0)
        return result
    }
}