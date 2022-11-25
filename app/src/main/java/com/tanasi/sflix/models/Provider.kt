package com.tanasi.sflix.models

import com.tanasi.sflix.adapters.SflixAdapter

class Provider(
    val name: String,
    val logo: String,

    val provider: com.tanasi.sflix.providers.Provider,
) : SflixAdapter.Item {


    override var itemType = SflixAdapter.Type.PROVIDER
}