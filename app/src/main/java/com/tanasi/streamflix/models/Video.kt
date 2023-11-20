package com.tanasi.streamflix.models

class Video(
    val source: String,

    val subtitles: List<Subtitle> = listOf(),
) {

    class Subtitle(
        val label: String,
        val file: String,
    )

    class Server(
        val id: String,
        val name: String,
        val src: String = "",
    )
}