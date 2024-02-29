package com.tanasi.navigation.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import androidx.appcompat.view.SupportMenuInflater
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.content.res.getResourceIdOrThrow
import com.tanasi.navigation.R

class NavigationSlideView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : FrameLayout(context, attrs, defStyle) {

    val menu = NavigationSlideMenu(context)
    var headerView: NavigationSlideHeaderView? = null
    val menuView = NavigationSlideMenuView(context).also {
        it.navigationSlideView = this
    }
    private val presenter = NavigationSlidePresenter()
    private val menuInflater: MenuInflater = SupportMenuInflater(context)

    var isOpen = true

    private var selectedListener: ((item: MenuItem) -> Boolean)? = null
    private var reselectedListener: ((item: MenuItem) -> Boolean)? = null

    /**
     * Currently selected menu item ID, or zero if there is no menu.
     */
    var selectedItemId: Int
        get() = menuView.selectedItemId
        set(value) {
            val item = menu.findItem(value)
            if (item != null) {
                if (!menu.performItemAction(item, presenter, 0)) {
                    item.isChecked = true
                }
            }
        }

    /**
     * Current gravity setting for how destinations in the menu view will be grouped.
     */
    private var menuGravity: Int
        get() = menuView.menuGravity
        set(value) {
            menuView.menuGravity = value
        }

    /**
     * Current spacing setting for how destinations in the menu view will be spaced out.
     */
    private var menuSpacing: Int
        get() = menuView.menuSpacing
        set(value) {
            menuView.menuSpacing = value
        }


    init {
        val attributes = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.NavigationSlideView,
            0,
            0
        )

        presenter.menuView = menuView
        menuView.presenter = presenter
        menu.addMenuPresenter(presenter)
        presenter.initForMenu(getContext(), menu)

        val headerLayoutRes = attributes.getResourceId(
            R.styleable.NavigationSlideView_headerLayout,
            0
        )
        if (headerLayoutRes != 0) {
            addHeaderView(headerLayoutRes)
        }

        menuGravity = attributes.getInt(
            R.styleable.NavigationSlideView_menuGravity,
            DEFAULT_MENU_GRAVITY
        )

        menuSpacing = attributes.getDimensionPixelSize(
            R.styleable.NavigationSlideView_menuSpacing,
            DEFAULT_MENU_SPACING
        )

        inflateMenu(attributes.getResourceIdOrThrow(R.styleable.NavigationSlideView_menu))

        attributes.recycle()

        addView(menuView)

        menu.setCallback(object : MenuBuilder.Callback {
            override fun onMenuItemSelected(menu: MenuBuilder, item: MenuItem): Boolean {
                if (reselectedListener != null && item.itemId == selectedItemId) {
                    reselectedListener?.invoke(item)
                    return true
                }
                return !(selectedListener?.invoke(item) ?: true)
            }

            override fun onMenuModeChange(menu: MenuBuilder) {}
        })
    }


    fun setOnItemSelectedListener(onNavigationItemSelected: (item: MenuItem) -> Boolean) {
        selectedListener = onNavigationItemSelected
    }

    fun setOnItemReselectedListener(onNavigationItemReselected: (item: MenuItem) -> Boolean) {
        reselectedListener = onNavigationItemReselected
    }

    /**
     * Inflate a menu resource into this navigation view.
     *
     *
     * Existing items in the menu will not be modified or removed.
     *
     * @param resId ID of a menu resource to inflate
     */
    fun inflateMenu(resId: Int) {
        presenter.updateSuspended = true
        menuInflater.inflate(resId, menu)
        presenter.updateSuspended = false
        presenter.updateMenuView(true)
    }

    fun buildNavigation() {
        headerView?.setOnFocusChangeListener { _, _ ->
            when {
                headerView?.hasFocus() == true || menuView.hasFocus() -> open()
                else -> close()
            }
        }
        menuView.forEach { child, item ->
            child.setOnFocusChangeListener { _, _ ->
                when {
                    headerView?.hasFocus() == true || menuView.hasFocus() -> open()
                    else -> close()
                }
            }

            child.setOnClickListener {
                if (!menu.performItemAction(item, presenter, 0)) {
                    item.isChecked = true
                }
            }
        }

        when {
            isOpen -> open()
            else -> close()
        }
    }

    fun addHeaderView(@LayoutRes layoutRes: Int) {
        val headerView = LayoutInflater.from(context).inflate(layoutRes, this, false)
        if (headerView is NavigationSlideHeaderView) {
            addHeaderView(headerView)
        }
    }

    fun addHeaderView(headerView: NavigationSlideHeaderView) {
        removeHeaderView()
        this.headerView = headerView
        addView(headerView, 0)
    }

    fun removeHeaderView() {
        if (headerView != null) {
            removeView(headerView)
            headerView = null
        }
    }

    fun open() {
        isOpen = true

        headerView?.open()
        menuView.open()
    }

    fun close() {
        isOpen = false

        headerView?.close()
        menuView.close()
    }


    companion object {
        const val DEFAULT_MENU_GRAVITY = Gravity.TOP or Gravity.START
        const val DEFAULT_MENU_SPACING = 0
    }
}