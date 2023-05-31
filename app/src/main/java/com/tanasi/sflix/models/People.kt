package com.tanasi.sflix.models

import com.tanasi.sflix.adapters.AppAdapter

class People(
    val id: String,
    val name: String,
    val image: String = "",

    val filmography: List<Show> = listOf(),
) : AppAdapter.Item {

    override var itemType = AppAdapter.Type.PEOPLE_ITEM
}