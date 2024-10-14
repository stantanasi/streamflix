package com.tanasi.streamflix.models

import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.utils.format
import com.tanasi.streamflix.utils.toCalendar
import java.util.Calendar

class People(
    val id: String,
    val name: String,
    val image: String? = null,
    val biography: String? = null,
    val placeOfBirth: String? = null,
    birthday: String? = null,
    deathday: String? = null,

    val filmography: List<Show> = listOf(),
) : AppAdapter.Item {

    val birthday: Calendar? = birthday?.toCalendar()
    val deathday: Calendar? = deathday?.toCalendar()

    override lateinit var itemType: AppAdapter.Type


    fun copy(
        id: String = this.id,
        name: String = this.name,
        image: String? = this.image,
        biography: String? = this.biography,
        placeOfBirth: String? = this.placeOfBirth,
        birthday: String? = this.birthday?.format("yyyy-MM-dd"),
        deathday: String? = this.deathday?.format("yyyy-MM-dd"),
        filmography: List<Show> = this.filmography,
    ) = People(
        id,
        name,
        image,
        biography,
        placeOfBirth,
        birthday,
        deathday,
        filmography,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as People

        if (id != other.id) return false
        if (name != other.name) return false
        if (image != other.image) return false
        if (biography != other.biography) return false
        if (placeOfBirth != other.placeOfBirth) return false
        if (birthday != other.birthday) return false
        if (deathday != other.deathday) return false
        if (filmography != other.filmography) return false
        if (!::itemType.isInitialized || !other::itemType.isInitialized) return false
        return itemType == other.itemType
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (image?.hashCode() ?: 0)
        result = 31 * result + (biography?.hashCode() ?: 0)
        result = 31 * result + (placeOfBirth?.hashCode() ?: 0)
        result = 31 * result + (birthday?.hashCode() ?: 0)
        result = 31 * result + (deathday?.hashCode() ?: 0)
        result = 31 * result + filmography.hashCode()
        result = 31 * result + (if (::itemType.isInitialized) itemType.hashCode() else 0)
        return result
    }
}