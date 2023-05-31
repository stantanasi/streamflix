package com.tanasi.streamflix.models

import com.tanasi.streamflix.adapters.AppAdapter

class Genre(
    val id: String,
    val name: String,

    val shows: List<Show> = listOf(),
) : AppAdapter.Item {

    override lateinit var itemType: AppAdapter.Type
}