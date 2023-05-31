package com.tanasi.sflix.models

import com.tanasi.sflix.adapters.AppAdapter

class Season(
    val id: String,
    val number: Int,
    val title: String = "",
    val poster: String = "",

    var tvShow: TvShow? = null,
    var episodes: List<Episode> = listOf(),
) : AppAdapter.Item {


    override var itemType = AppAdapter.Type.SEASON_ITEM
}