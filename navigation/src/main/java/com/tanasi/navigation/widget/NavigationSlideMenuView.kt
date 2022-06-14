package com.tanasi.navigation.widget

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.appcompat.view.menu.MenuView
import androidx.core.view.forEachIndexed
import kotlin.math.min

@SuppressLint("RestrictedApi")
class NavigationSlideMenuView(
    context: Context,
) : LinearLayout(context), MenuView {

    private var childs = mutableListOf<NavigationSlideItemView>()
    var selectedItemId = 0
    var selectedItemPosition = 0

    lateinit var presenter: NavigationSlidePresenter
    private lateinit var menu: MenuBuilder

    private val layoutParams = FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )


    init {
        orientation = VERTICAL
        setLayoutParams(layoutParams)
    }


    override fun initialize(menu: MenuBuilder) {
        this.menu = menu
    }

    override fun getWindowAnimations(): Int = 0


    fun buildMenuView() {
        removeAllViews()

        childs.clear()

        menu.forEachIndexed { i, item ->
            presenter.updateSuspended = true
            item.isCheckable = true
            presenter.updateSuspended = false

            val child = NavigationSlideItemView(context)
            childs.add(child)

            child.isFocusable = true
            child.isFocusableInTouchMode = true

            child.initialize(item as MenuItemImpl, 0)
            child.itemPosition = i

            child.setOnClickListener {
                if (!menu.performItemAction(item, presenter, 0)) {
                    item.isChecked = true
                }
            }

            addView(child)
        }
        selectedItemPosition = min(menu.size() - 1, selectedItemPosition)
        menu.getItem(selectedItemPosition).isChecked = true
    }

    fun updateMenuView() {
        if (menu.size() != childs.size) {
            // The size has changed. Rebuild menu view from scratch.
            buildMenuView()
            return
        }

        menu.forEachIndexed { i, item ->
            if (item.isChecked) {
                selectedItemId = item.itemId
                selectedItemPosition = i
            }
        }

        childs.forEachIndexed { i, child ->
            presenter.updateSuspended = true
            child.initialize((menu.getItem(i) as MenuItemImpl), 0)
            presenter.updateSuspended = false
        }
    }
}