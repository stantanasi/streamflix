package com.tanasi.sflix.models

import com.tanasi.sflix.adapters.SflixAdapter

class Season(
    val id: String,
    val number: Int,
    val title: String = "",
    val poster: String = "",

    var episodes: List<Episode> = listOf(),
    var tvShow: TvShow? = null,
) : SflixAdapter.Item {


    override var itemType = SflixAdapter.Type.SEASON
}