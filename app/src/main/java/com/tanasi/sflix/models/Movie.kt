package com.tanasi.sflix.models

class Movie(
    val id: String,
    val title: String,
    val year: Int,
    val quality: String,
    val rating: Double,
    val poster: String,
    val banner: String? = null,
) {
}