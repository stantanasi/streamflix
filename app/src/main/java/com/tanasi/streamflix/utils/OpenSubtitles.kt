package com.tanasi.streamflix.utils

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object OpenSubtitles {

    private const val URL = "https://rest.opensubtitles.org/"

    private val service = Service.build()


    private interface Service {

        companion object {
            fun build(): Service {
                val retrofit = Retrofit.Builder()
                    .baseUrl(URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                return retrofit.create(Service::class.java)
            }
        }
    }
}