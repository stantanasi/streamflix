package com.tanasi.sflix.models

import com.tanasi.sflix.adapters.AppAdapter

class Category(
    val name: String,
    val list: List<AppAdapter.Item>,
) : AppAdapter.Item {

    var selectedIndex = 0
    var itemSpacing: Int = 0


    override var itemType = AppAdapter.Type.CATEGORY_ITEM
}