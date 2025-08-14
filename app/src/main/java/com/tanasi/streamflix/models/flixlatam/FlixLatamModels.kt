package com.tanasi.streamflix.models.flixlatam

import kotlinx.serialization.Serializable

@Serializable
data class PlayerResponse(
    val embed_url: String = "",
    val type: String = "",
)

@Serializable
data class DataLinkItem(
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