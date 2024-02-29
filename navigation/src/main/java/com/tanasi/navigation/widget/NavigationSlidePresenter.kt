package com.tanasi.navigation.widget

import android.content.Context
import android.os.Parcelable
import android.view.ViewGroup
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.appcompat.view.menu.MenuPresenter
import androidx.appcompat.view.menu.MenuView
import androidx.appcompat.view.menu.SubMenuBuilder

class NavigationSlidePresenter : MenuPresenter {

    private lateinit var menu: MenuBuilder
    lateinit var menuView: NavigationSlideMenuView
    var updateSuspended = false

    override fun initForMenu(context: Context, menu: MenuBuilder) {
        this.menu = menu
        menuView.initialize(this.menu)
    }

    override fun getMenuView(root: ViewGroup?): MenuView = menuView

    override fun updateMenuView(cleared: Boolean) {
        if (updateSuspended) return

        when {
            cleared -> menuView.buildMenuView()
            else -> menuView.updateMenuView()
        }
    }

    override fun setCallback(cb: MenuPresenter.Callback?) {}

    override fun onSubMenuSelected(subMenu: SubMenuBuilder?): Boolean = false

    override fun onCloseMenu(menu: MenuBuilder?, allMenusAreClosing: Boolean) {}

    override fun flagActionItems(): Boolean = false

    override fun expandItemActionView(menu: MenuBuilder?, item: MenuItemImpl?): Boolean = false

    override fun collapseItemActionView(menu: MenuBuilder?, item: MenuItemImpl?): Boolean = false

    override fun getId(): Int = MENU_PRESENTER_ID

    override fun onSaveInstanceState(): Parcelable {
        TODO("Not yet implemented")
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        TODO("Not yet implemented")
    }


    companion object {
        const val MENU_PRESENTER_ID = 1
    }
}