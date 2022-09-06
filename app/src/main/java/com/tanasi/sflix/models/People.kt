package com.tanasi.sflix.models

import com.tanasi.sflix.adapters.SflixAdapter

class People(
    val slug: String,
    val name: String,
    val image: String = "",

    val movies: List<Movie> = listOf(),
    val tvShows: List<TvShow> = listOf(),
) : SflixAdapter.Item {

    override var itemType = SflixAdapter.Type.PEOPLE
}