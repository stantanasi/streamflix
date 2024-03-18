package com.tanasi.streamflix.models

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.tanasi.streamflix.adapters.AppAdapter

@Entity("seasons")
class Season(
    @PrimaryKey
    var id: String,
    var number: Int,
    var title: String = "",
    var poster: String = "",

    var tvShow: TvShow? = null,
    @Ignore
    var episodes: List<Episode> = listOf(),
) : AppAdapter.Item {

    constructor() : this("", 0)


    @Ignore
    override var itemType = AppAdapter.Type.SEASON_ITEM

    fun copy(
        id: String = this.id,
        number: Int = this.number,
        title: String = this.title,
        poster: String = this.poster,
        tvShow: TvShow? = this.tvShow,
        episodes: List<Episode> = this.episodes,
    ) = Season(
        id,
        number,
        title,
        poster,
        tvShow,
        episodes,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Season

        if (id != other.id) return false
        if (number != other.number) return false
        if (title != other.title) return false
        if (poster != other.poster) return false
        if (tvShow != other.tvShow) return false
        if (episodes != other.episodes) return false
        return itemType == other.itemType
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + number
        result = 31 * result + title.hashCode()
        result = 31 * result + poster.hashCode()
        result = 31 * result + (tvShow?.hashCode() ?: 0)
        result = 31 * result + episodes.hashCode()
        result = 31 * result + itemType.hashCode()
        return result
    }
}