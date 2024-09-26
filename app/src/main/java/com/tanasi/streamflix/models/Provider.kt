package com.tanasi.streamflix.models

import com.tanasi.streamflix.adapters.AppAdapter

class Provider(
    val name: String,
    val logo: String,
    val language: String,

    val provider: com.tanasi.streamflix.providers.Provider,
) : AppAdapter.Item {


    override lateinit var itemType: AppAdapter.Type
}