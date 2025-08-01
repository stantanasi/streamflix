package com.tanasi.streamflix.models.sololatino

import kotlinx.serialization.Serializable

@Serializable
data class Item(
    val file_id: Int,
    val video_language: String,
    val sortedEmbeds: List<Embed>,
)

@Serializable
data class Embed(
    val servername: String,
    val link: String,
    val type: String,
)