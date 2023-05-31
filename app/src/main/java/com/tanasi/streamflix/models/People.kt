package com.tanasi.streamflix.models

import com.tanasi.streamflix.adapters.AppAdapter

class People(
    val id: String,
    val name: String,
    val image: String = "",

    val filmography: List<Show> = listOf(),
) : AppAdapter.Item {

    override var itemType = AppAdapter.Type.PEOPLE_ITEM
}