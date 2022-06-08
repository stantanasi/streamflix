package com.tanasi.sflix.models

class Video(
    val source: String,

    val subtitles: List<Subtitle> = listOf(),
) {

    class Subtitle(
        val label: String,
        val file: String,
        val default: Boolean = false,
    ) {
    }
}