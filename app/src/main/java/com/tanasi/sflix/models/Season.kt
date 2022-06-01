package com.tanasi.sflix.models

import com.tanasi.sflix.adapters.SflixAdapter

class Season(
    val id: String,
    val number: Int,
    val title: String = "",

    val episodes: List<Episode> = listOf(),
) : SflixAdapter.Item {


    override lateinit var itemType: SflixAdapter.Type
}