package com.tanasi.streamflix.models.animeflv

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServerModel(
    @SerialName("SUB")
    val sub: List<Sub> = emptyList(),
)

@Serializable
data class Sub(
    val title: String? = "",
    val code: String = "",
)