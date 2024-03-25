package com.tanasi.streamflix.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

class Video(
    val source: String,

    val subtitles: List<Subtitle> = listOf(),
) {

    sealed class Type : Parcelable {
        @Parcelize
        data class Movie(
            val id: String,
            val title: String,
            val releaseDate: String,
            val poster: String,
        ) : Type()

        @Parcelize
        data class Episode(
            val id: String,
            val number: Int,
            val title: String?,
            val poster: String?,
            val tvShow: TvShow,
            val season: Season,
        ) : Type() {
            @Parcelize
            data class TvShow(
                val id: String,
                val title: String,
                val poster: String?,
                val banner: String?,
            ) : Parcelable

            @Parcelize
            data class Season(
                val number: Int,
                val title: String?,
            ) : Parcelable
        }
    }

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