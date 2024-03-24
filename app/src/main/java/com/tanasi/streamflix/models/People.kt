package com.tanasi.streamflix.models

import com.tanasi.streamflix.adapters.AppAdapter

class People(
    val id: String,
    val name: String,
    val image: String? = null,

    val filmography: List<Show> = listOf(),
) : AppAdapter.Item {

    override lateinit var itemType: AppAdapter.Type


    fun copy(
        id: String = this.id,
        name: String = this.name,
        image: String? = this.image,
        filmography: List<Show> = this.filmography,
    ) = People(
        id,
        name,
        image,
        filmography,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as People

        if (id != other.id) return false
        if (name != other.name) return false
        if (image != other.image) return false
        if (filmography != other.filmography) return false
        if (!::itemType.isInitialized || !other::itemType.isInitialized) return false
        return itemType == other.itemType
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (image?.hashCode() ?: 0)
        result = 31 * result + filmography.hashCode()
        result = 31 * result + (if (::itemType.isInitialized) itemType.hashCode() else 0)
        return result
    }
}