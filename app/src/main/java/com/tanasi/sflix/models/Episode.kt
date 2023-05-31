package com.tanasi.sflix.models

import com.tanasi.sflix.adapters.AppAdapter
import com.tanasi.sflix.utils.toCalendar

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