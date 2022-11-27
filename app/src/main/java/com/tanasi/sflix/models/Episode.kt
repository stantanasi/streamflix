package com.tanasi.sflix.models

import com.tanasi.sflix.adapters.SflixAdapter
import com.tanasi.sflix.utils.toCalendar

class Episode(
    val id: String,
    val number: Int,
    val title: String = "",
    released: String? = null,
    val poster: String? = null,

    var tvShow: TvShow? = null,
    var season: Season? = null,
) : SflixAdapter.Item {

    val released = released?.toCalendar()


    override var itemType = SflixAdapter.Type.EPISODE_ITEM
}