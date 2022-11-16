package com.tanasi.sflix.models

import com.tanasi.sflix.adapters.SflixAdapter

class Episode(
    val id: String,
    val number: Int,
    val title: String = "",
    val poster: String? = null,

    var tvShow: TvShow? = null,
    var season: Season? = null,
) : SflixAdapter.Item {


    override var itemType = SflixAdapter.Type.EPISODE_ITEM
}