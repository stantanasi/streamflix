package com.tanasi.navigation.widget

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.core.view.forEach
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.onNavDestinationSelected
import java.lang.ref.WeakReference

/**
 * Sets up a [NavigationSlideView] for use with a [NavController]. This will call
 * [android.view.MenuItem.onNavDestinationSelected] when a menu item is selected.
 *
 * The selected item in the NavigationSlideView will automatically be updated when the destination
 * changes.
 */
fun NavigationSlideView.setupWithNavController(navController: NavController) {
    NavigationUI.setupWithNavController(this, navController)
}

fun NavigationUI.setupWithNavController(
    navigationSlideView: NavigationSlideView,
    navController: NavController
) {
    navigationSlideView.setOnItemSelectedListener { item ->
        onNavDestinationSelected(
            item,
            navController
        )
        navController.popBackStack(item.itemId, inclusive = false)
        true
    }
    navigationSlideView.setOnItemReselectedListener { item ->
        navController.popBackStack(item.itemId, inclusive = true)
        navController.navigate(item.itemId)
        true
    }
    val weakReference = WeakReference(navigationSlideView)
    navController.addOnDestinationChangedListener(
        object : NavController.OnDestinationChangedListener {
            override fun onDestinationChanged(
                controller: NavController,
                destination: NavDestination,
                arguments: Bundle?
            ) {
                val view = weakReference.get()
                if (view == null) {
                    navController.removeOnDestinationChangedListener(this)
                    return
                }
                view.menu.forEach { item ->
                    if (destination.matchDestination(item.itemId)) {
                        item.isChecked = true
                    }
                }
            }
        })
}

fun NavDestination.matchDestination(@IdRes destId: Int): Boolean =
    hierarchy.any { it.id == destId }