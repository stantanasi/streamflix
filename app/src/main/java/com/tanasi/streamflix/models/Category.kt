package com.tanasi.streamflix.models

import com.tanasi.streamflix.adapters.AppAdapter

class Category(
    val name: String,
    val list: List<AppAdapter.Item>,
) : AppAdapter.Item {

    var selectedIndex = 0
    var itemSpacing: Int = 0


    override var itemType = AppAdapter.Type.CATEGORY_ITEM


    companion object {
        const val FEATURED = "Featured"
    }
}