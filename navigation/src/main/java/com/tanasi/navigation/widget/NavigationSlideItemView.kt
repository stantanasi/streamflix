package com.tanasi.navigation.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.FrameLayout
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.appcompat.view.menu.MenuView.ItemView

@SuppressLint("RestrictedApi")
class NavigationSlideItemView(
    context: Context,
) : FrameLayout(context), ItemView {

    override fun initialize(itemData: MenuItemImpl?, menuType: Int) {
        TODO("Not yet implemented")
    }

    override fun getItemData(): MenuItemImpl {
        TODO("Not yet implemented")
    }

    override fun setTitle(title: CharSequence?) {
        TODO("Not yet implemented")
    }

    override fun setCheckable(checkable: Boolean) {
        TODO("Not yet implemented")
    }

    override fun setChecked(checked: Boolean) {
        TODO("Not yet implemented")
    }

    override fun setShortcut(showShortcut: Boolean, shortcutKey: Char) {
        TODO("Not yet implemented")
    }

    override fun setIcon(icon: Drawable?) {
        TODO("Not yet implemented")
    }

    override fun prefersCondensedTitle(): Boolean {
        TODO("Not yet implemented")
    }

    override fun showsIcon(): Boolean {
        TODO("Not yet implemented")
    }
}