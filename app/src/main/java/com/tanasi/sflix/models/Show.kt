package com.tanasi.sflix.models

import com.tanasi.sflix.adapters.SflixAdapter
import com.tanasi.sflix.utils.toCalendar

sealed class Show : SflixAdapter.Item {

    enum class Type {
        Movie,
        TvShow;
    }
}

class Movie(
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

    val genres: List<Genre> = listOf(),
    val cast: List<People> = listOf(),
    val recommendations: List<Show> = listOf(),
) : Show(), SflixAdapter.Item, Cloneable {

    val released = released?.toCalendar()


    override lateinit var itemType: SflixAdapter.Type

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
    val cast: List<People> = listOf(),
    val recommendations: List<Show> = listOf(),
) : Show(), SflixAdapter.Item, Cloneable {

    val released = released?.toCalendar()


    override lateinit var itemType: SflixAdapter.Type

    public override fun clone() = super.clone() as TvShow
}
