package com.tanasi.sflix.models

import com.tanasi.sflix.adapters.SflixAdapter

class People(
    val slug: String,
    val name: String,
    val image: String = "",
) : SflixAdapter.Item {

    override lateinit var itemType: SflixAdapter.Type
}