package com.tanasi.sflix.models

class Season(
    val id: String,
    val number: Int,
    val title: String = "",

    val episodes: List<Episode> = listOf(),
) {
}