package com.tanasi.sflix.models

class Movie(
    val id: String,
    val title: String,
    val year: Int,
    quality: String,
    val rating: Double,
    val poster: String,
    val banner: String? = null,
) {

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