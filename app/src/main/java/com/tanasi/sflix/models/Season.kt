package com.tanasi.sflix.models

import com.tanasi.sflix.adapters.SflixAdapter

class Season(
    val id: String,
    val number: Int,
    val title: String = "",
    val poster: String = "",

    var tvShow: TvShow? = null,
    var episodes: List<Episode> = listOf(),
) : SflixAdapter.Item {


    override var itemType = SflixAdapter.Type.SEASON_ITEM
}