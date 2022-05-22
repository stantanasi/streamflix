package com.tanasi.sflix.models

import com.tanasi.sflix.utils.toCalendar

class Movie(
    val id: String,
    val title: String,
    released: String,
    quality: String,
    val rating: Double,
    val poster: String,
    val banner: String? = null,
) {

    val released = released.toCalendar()
    val quality = Quality.getByValue(quality)


    enum class Quality {
        HD,
        CAM;

        companion object {
            fun getByValue(value: String): Quality? = try {
                valueOf(value)
            } catch (e: Exception) {
                null
            }
        }
    }
}