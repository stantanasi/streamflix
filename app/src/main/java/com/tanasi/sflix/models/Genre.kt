package com.tanasi.sflix.models

import com.tanasi.sflix.adapters.AppAdapter

class Genre(
    val id: String,
    val name: String,

    val shows: List<Show> = listOf(),
) : AppAdapter.Item {

    override lateinit var itemType: AppAdapter.Type
}