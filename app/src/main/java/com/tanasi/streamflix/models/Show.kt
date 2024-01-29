package com.tanasi.streamflix.models

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.utils.toCalendar

sealed class Show : AppAdapter.Item

@Entity("movies")
class Movie(
    @PrimaryKey
    var id: String,
    var title: String,
    var overview: String = "",
    released: String? = null,
    var runtime: Int? = null,
    var youtubeTrailerId: String? = null,
    var quality: String? = null,
    var rating: Double? = null,
    var poster: String? = null,
    var banner: String? = null,
    var isFavorite: Boolean = false,

    @Ignore
    val genres: List<Genre> = listOf(),
    @Ignore
    val directors: List<People> = listOf(),
    @Ignore
    val cast: List<People> = listOf(),
    @Ignore
    val recommendations: List<Show> = listOf(),
) : Show(), AppAdapter.Item, Cloneable {

    constructor() : this("", "")

    var released = released?.toCalendar()


    @Ignore
    override lateinit var itemType: AppAdapter.Type

    public override fun clone() = super.clone() as Movie
}

class TvShow(
    val id: String,
    val title: String,
    val overview: String = "",
    released: String? = null,
    val runtime: Int? = null,
    val youtubeTrailerId: String? = null,
    val quality: String? = null,
    val rating: Double? = null,
    val poster: String? = null,
    val banner: String? = null,

    val seasons: List<Season> = listOf(),
    val genres: List<Genre> = listOf(),
    val directors: List<People> = listOf(),
    val cast: List<People> = listOf(),
    val recommendations: List<Show> = listOf(),
) : Show(), AppAdapter.Item, Cloneable {

    val released = released?.toCalendar()


    override lateinit var itemType: AppAdapter.Type

    public override fun clone() = super.clone() as TvShow
}
