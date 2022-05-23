package com.tanasi.sflix.models

class TvShow(
    val id: String,
    val title: String,
    val lastEpisode: String,
    val quality: String,
    val rating: Double?,
    val poster: String,
    val banner: String? = null,
) {
}