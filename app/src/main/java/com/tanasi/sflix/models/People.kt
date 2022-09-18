package com.tanasi.sflix.models

import com.tanasi.sflix.adapters.SflixAdapter

class People(
    val slug: String,
    val name: String,
    val image: String = "",

    val filmography: List<Show> = listOf(),
) : SflixAdapter.Item {

    override var itemType = SflixAdapter.Type.PEOPLE_ITEM
}