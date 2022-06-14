package com.tanasi.navigation.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.appcompat.view.menu.MenuView.ItemView
import com.tanasi.navigation.R

@SuppressLint("RestrictedApi")
class NavigationSlideItemView(
    context: Context,
) : FrameLayout(context), ItemView {

    private val view = LayoutInflater.from(context).inflate(
        getItemLayoutResId(),
        this,
        true
    )

    private val icon = view.findViewById<ImageView>(R.id.iv_navigation_item_icon)
    private val label = view.findViewById<TextView>(R.id.tv_navigation_item_label)


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


    /**
     * Returns the unique identifier to the layout resource that must be used to render the items in
     * this menu item view.
     */
    fun getItemLayoutResId(): Int = R.layout.item_navigation
}