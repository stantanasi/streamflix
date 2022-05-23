package com.tanasi.sflix.models

class Video(
    val source: String,

    val subtitles: List<Subtitle> = listOf(),
) {

    class Subtitle(
        val name: String,
        val file: String,
    ) {
    }
}