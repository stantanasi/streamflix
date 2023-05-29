package com.tanasi.sflix.activities

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.NavHostFragment
import com.bumptech.glide.Glide
import com.tanasi.navigation.widget.setupWithNavController
import com.tanasi.sflix.NavMainGraphDirections
import com.tanasi.sflix.R
import com.tanasi.sflix.databinding.ActivityMainBinding
import com.tanasi.sflix.databinding.ContentHeaderMenuMainBinding
import com.tanasi.sflix.fragments.player.PlayerFragment
import com.tanasi.sflix.utils.UserPreferences

class MainActivity : FragmentActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding: ActivityMainBinding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(binding.navMainFragment.id) as NavHostFragment
        val navController = navHostFragment.navController

        binding.navMain.setupWithNavController(navController)

        binding.navMain.headerView?.apply {
            val header = ContentHeaderMenuMainBinding.bind(this)

            Glide.with(context)
                .load(UserPreferences.currentProvider.logo)
                .into(header.ivNavigationHeaderIcon)
            header.tvNavigationHeaderTitle.text = UserPreferences.currentProvider.name
            header.tvNavigationHeaderSubtitle.text = getString(R.string.main_menu_change_provider)

            setOnOpenListener {
                header.tvNavigationHeaderTitle.visibility = View.VISIBLE
                header.tvNavigationHeaderSubtitle.visibility = View.VISIBLE
            }
            setOnCloseListener {
                header.tvNavigationHeaderTitle.visibility = View.GONE
                header.tvNavigationHeaderSubtitle.visibility = View.GONE
            }

            setOnClickListener {
                navController.navigate(NavMainGraphDirections.actionGlobalProviders())
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.search,
                R.id.home,
                R.id.movies,
                R.id.tv_shows -> binding.navMain.visibility = View.VISIBLE
                else -> binding.navMain.visibility = View.GONE
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (navController.currentDestination?.id) {
                    R.id.home -> when {
                        binding.navMain.hasFocus() -> finish()
                        else -> binding.navMain.requestFocus()
                    }
                    R.id.search,
                    R.id.movies,
                    R.id.tv_shows -> when {
                        binding.navMain.hasFocus() -> binding.navMain.findViewById<View>(R.id.home)
                            .let {
                                it.requestFocus()
                                it.performClick()
                            }
                        else -> binding.navMain.requestFocus()
                    }
                    else -> {
                        val currentFragment = navHostFragment.childFragmentManager.fragments
                            .firstOrNull()
                        when (currentFragment) {
                            is PlayerFragment -> currentFragment.onBackPressed()
                            else -> false
                        }.takeIf { !it }?.let {
                            navController.navigateUp()
                        }
                    }
                }
            }
        })
    }
}