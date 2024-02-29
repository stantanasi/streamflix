package com.tanasi.navigation.widget

import android.content.Context
import android.view.MenuItem
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuItemImpl

class NavigationSlideMenu(
    context: Context,
) : MenuBuilder(context) {

    override fun addInternal(
        group: Int, id: Int, categoryOrder: Int, title: CharSequence
    ): MenuItem {
        stopDispatchingItemsChanged()
        val item = super.addInternal(group, id, categoryOrder, title)
        if (item is MenuItemImpl) {
            item.isExclusiveCheckable = true
        }
        startDispatchingItemsChanged()
        return item
    }
}