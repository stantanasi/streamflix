package com.tanasi.sflix.models

import com.tanasi.sflix.adapters.SflixAdapter

class Row(
    val title: String,
    val list: List<SflixAdapter.Item>,
) : SflixAdapter.Item {

    override var itemType = SflixAdapter.Type.ROW
}