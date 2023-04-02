package com.tanasi.sflix.utils

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object GitHub {

    val service = ApiService.build()

    interface ApiService {

        companion object {
            fun build(): ApiService {
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://api.github.com/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                return retrofit.create(ApiService::class.java)
            }
        }
    }
}