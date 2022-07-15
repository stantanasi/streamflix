package com.tanasi.sflix.models

import com.tanasi.sflix.adapters.SflixAdapter
import com.tanasi.sflix.utils.toCalendar

class TvShow(
    val id: String,
    val title: String,
    val overview: String = "",
    released: String? = null,
    val runtime: Int? = null,
    val youtubeTrailerId: String? = null,
    quality: String = "",
    val rating: Double? = null,
    val poster: String? = null,
    val banner: String? = null,

    val seasons: List<Season> = listOf(),
) : SflixAdapter.Item {

    val released = released?.toCalendar()
    val quality = Quality.getByValue(quality)


    enum class Quality {
        HD,
        SD,
        CAM;

        companion object {
            fun getByValue(value: String): Quality? = try {
                valueOf(value)
            } catch (e: Exception) {
                null
            }
        }
    }


    override lateinit var itemType: SflixAdapter.Type
}