package com.tanasi.streamflix.models

class Video(
    val sources: List<String>,

    val subtitles: List<Subtitle> = listOf(),
) {

    class Subtitle(
        val label: String,
        val file: String,
        val default: Boolean = false,
    )
}