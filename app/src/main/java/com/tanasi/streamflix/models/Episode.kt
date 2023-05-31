package com.tanasi.streamflix.models

import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.utils.toCalendar

class Episode(
    val id: String,
    val number: Int,
    val title: String = "",
    released: String? = null,
    val poster: String? = null,

    var tvShow: TvShow? = null,
    var season: Season? = null,
) : AppAdapter.Item {

    val released = released?.toCalendar()


    override var itemType = AppAdapter.Type.EPISODE_ITEM
}