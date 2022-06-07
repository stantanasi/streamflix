package com.tanasi.sflix.activities

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.tanasi.sflix.R
import com.tanasi.sflix.databinding.ActivityMainBinding

class MainActivity : FragmentActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding: ActivityMainBinding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navController = (supportFragmentManager
            .findFragmentById(binding.navMainFragment.id) as NavHostFragment)
            .navController

        binding.navMain.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.player -> binding.navMain.visibility = View.GONE
                else -> binding.navMain.visibility = View.VISIBLE
            }
        }
    }

    override fun onBackPressed() {
        val navController = (supportFragmentManager
            .findFragmentById(binding.navMainFragment.id) as NavHostFragment)
            .navController

        when (navController.currentDestination?.id) {
            R.id.search,
            R.id.home -> finish()
            else -> super.onBackPressed()
        }
    }
}