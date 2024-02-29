package com.tanasi.navigation.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.appcompat.view.menu.MenuView.ItemView
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.PointerIconCompat
import androidx.core.view.ViewCompat
import com.tanasi.navigation.R

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

    var itemPosition = INVALID_ITEM_POSITION

    private lateinit var itemData: MenuItemImpl


    override fun initialize(itemData: MenuItemImpl, menuType: Int) {
        this.itemData = itemData
        setCheckable(itemData.isCheckable)
        setChecked(itemData.isChecked)
        isEnabled = itemData.isEnabled
        setIcon(itemData.icon)
        setTitle(itemData.title)
        id = itemData.itemId
        if (!TextUtils.isEmpty(itemData.contentDescription)) {
            contentDescription = itemData.contentDescription
        }

        val tooltipText = when {
            itemData.tooltipText?.isNotEmpty() == true -> itemData.tooltipText
            else -> itemData.title
        }

        // Avoid calling tooltip for L and M devices because long pressing twice may freeze devices.
        if (VERSION.SDK_INT > VERSION_CODES.M) {
            TooltipCompat.setTooltipText(this, tooltipText)
        }
        visibility = if (itemData.isVisible) VISIBLE else GONE
    }

    override fun getItemData(): MenuItemImpl = itemData

    override fun setTitle(title: CharSequence?) {
        label.text = title
    }

    override fun setCheckable(checkable: Boolean) {
        refreshDrawableState()
    }

    override fun setChecked(checked: Boolean) {
        refreshDrawableState()

        // Set the item as selected to send an AccessibilityEvent.TYPE_VIEW_SELECTED from View, so that
        // the item is read out as selected.
        isSelected = checked
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        label.isEnabled = enabled
        icon.isEnabled = enabled
        if (enabled) {
            ViewCompat.setPointerIcon(
                this, PointerIconCompat.getSystemIcon(context, PointerIconCompat.TYPE_HAND)
            )
        } else {
            ViewCompat.setPointerIcon(this, null)
        }
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        if (itemData.isCheckable && itemData.isChecked) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET)
        }
        return drawableState
    }

    override fun setShortcut(showShortcut: Boolean, shortcutKey: Char) {
    }

    override fun setIcon(drawable: Drawable?) {
        if (drawable === icon.drawable) {
            return
        }

        icon.setImageDrawable(drawable)
    }

    override fun prefersCondensedTitle(): Boolean = false

    override fun showsIcon(): Boolean = true


    /**
     * Returns the unique identifier to the layout resource that must be used to render the items in
     * this menu item view.
     */
    fun getItemLayoutResId(): Int = R.layout.item_navigation


    fun open() {
        label.visibility = View.VISIBLE
    }

    fun close() {
        label.visibility = View.GONE
    }


    companion object {
        private const val INVALID_ITEM_POSITION = -1
        private val CHECKED_STATE_SET = intArrayOf(android.R.attr.state_checked)
    }
}