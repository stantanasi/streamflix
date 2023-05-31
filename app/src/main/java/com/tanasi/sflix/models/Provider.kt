package com.tanasi.sflix.models

import com.tanasi.sflix.adapters.AppAdapter

class Provider(
    val name: String,
    val logo: String,

    val provider: com.tanasi.sflix.providers.Provider,
) : AppAdapter.Item {


    override var itemType = AppAdapter.Type.PROVIDER
}