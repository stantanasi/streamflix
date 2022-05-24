package com.tanasi.sflix.models

class Episode(
    val id: String,
    val number: Int,
    val title: String = "",
    val poster: String = "",

    val servers: List<Server> = listOf(),
) {
}