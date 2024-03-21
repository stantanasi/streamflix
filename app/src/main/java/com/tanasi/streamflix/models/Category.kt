package com.tanasi.streamflix.models

import com.tanasi.streamflix.adapters.AppAdapter

class Category(
    val name: String,
    val list: List<AppAdapter.Item>,
) : AppAdapter.Item {

    var selectedIndex: Int = 0
    var itemSpacing: Int = 0


    override lateinit var itemType: AppAdapter.Type


    fun copy(
        name: String = this.name,
        list: List<AppAdapter.Item> = this.list,
    ) = Category(
        name,
        list,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Category

        if (name != other.name) return false
        if (list != other.list) return false
        if (selectedIndex != other.selectedIndex) return false
        if (itemSpacing != other.itemSpacing) return false
        return itemType == other.itemType
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + list.hashCode()
        result = 31 * result + selectedIndex
        result = 31 * result + itemSpacing
        result = 31 * result + itemType.hashCode()
        return result
    }


    companion object {
        const val FEATURED = "Featured"
    }
}