package com.tanasi.sflix.models

import com.tanasi.sflix.adapters.SflixAdapter

class Category(
    val name: String,
    val list: List<Show>,
) : SflixAdapter.Item {

    var itemSpacing: Int = 0


    override var itemType = SflixAdapter.Type.CATEGORY_ITEM
}