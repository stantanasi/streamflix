package com.tanasi.streamflix.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class Video(
    val source: String,
    val subtitles: List<Subtitle> = listOf(),
    val headers: Map<String, String>? = null,
    val type: String? = null // androidx.media3.common.MimeTypes - https://developer.android.com/reference/kotlin/androidx/media3/common/MimeTypes
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

    data class Subtitle(
        val label: String,
        val file: String,
        var default: Boolean = false,
    )

    data class Server(
        val id: String,
        val name: String,
        val src: String = "",
    ) {
        var video: Video? = null
    }
}