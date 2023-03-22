package com.tanasi.sflix.models

import com.tanasi.sflix.adapters.SflixAdapter

class Genre(
    val id: String,
    val name: String,

    val shows: List<Show> = listOf(),
) : SflixAdapter.Item {

    override lateinit var itemType: SflixAdapter.Type
}