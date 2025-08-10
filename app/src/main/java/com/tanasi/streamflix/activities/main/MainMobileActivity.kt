package com.tanasi.streamflix.activities.main

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.tanasi.streamflix.BuildConfig
import com.tanasi.streamflix.R
import com.tanasi.streamflix.database.AppDatabase
import com.tanasi.streamflix.databinding.ActivityMainMobileBinding
import com.tanasi.streamflix.fragments.player.PlayerMobileFragment
import com.tanasi.streamflix.ui.UpdateAppMobileDialog
import com.tanasi.streamflix.utils.UserPreferences
import com.tanasi.streamflix.utils.getCurrentFragment
import kotlinx.coroutines.launch

class MainMobileActivity : FragmentActivity() {

    private var _binding: ActivityMainMobileBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<MainViewModel>()

    private lateinit var updateAppDialog: UpdateAppMobileDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_Mobile)
        super.onCreate(savedInstanceState)
        _binding = ActivityMainMobileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = this.supportFragmentManager
            .findFragmentById(binding.navMainFragment.id) as NavHostFragment
        val navController = navHostFragment.navController

        UserPreferences.setup(this)
        AppDatabase.setup(this)

        when (BuildConfig.APP_LAYOUT) {
            "mobile" -> {}
            "tv" -> {
                finish()
                startActivity(Intent(this, MainTvActivity::class.java))
            }
            else -> {
                if (packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)) {
                    finish()
                    startActivity(Intent(this, MainTvActivity::class.java))
                }
            }
        }

        UserPreferences.currentProvider?.let {
            navController.navigate(R.id.home)
        }


        viewModel.checkUpdate()

        binding.bnvMain.setupWithNavController(navController)
        binding.bnvMain.setOnItemReselectedListener { item ->
            navController.popBackStack(item.itemId, inclusive = true)
            navController.navigate(item.itemId)
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.search,
                R.id.home,
                R.id.movies,
                R.id.tv_shows,
                R.id.settings -> binding.bnvMain.visibility = View.VISIBLE
                else -> binding.bnvMain.visibility = View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.state.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED).collect { state ->
                when (state) {
                    MainViewModel.State.CheckingUpdate -> {}
                    is MainViewModel.State.SuccessCheckingUpdate -> {
                        updateAppDialog = UpdateAppMobileDialog(this@MainMobileActivity, state.newReleases).also {
                            it.setOnUpdateClickListener { _ ->
                                if (!it.isLoading) viewModel.downloadUpdate(this@MainMobileActivity, state.asset)
                            }
                            it.show()
                        }
                    }

                    MainViewModel.State.DownloadingUpdate -> updateAppDialog.isLoading = true
                    is MainViewModel.State.SuccessDownloadingUpdate -> {
                        viewModel.installUpdate(this@MainMobileActivity, state.apk)
                        updateAppDialog.hide()
                    }

                    MainViewModel.State.InstallingUpdate -> updateAppDialog.isLoading = true

                    is MainViewModel.State.FailedUpdate -> {
                        Toast.makeText(
                            this@MainMobileActivity,
                            state.error.message ?: "",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (navController.currentDestination?.id) {
                    R.id.home -> finish()
                    R.id.search,
                    R.id.movies,
                    R.id.tv_shows,
                    R.id.settings -> binding.bnvMain.findViewById<View>(R.id.home).performClick()
                    else -> navController.navigateUp()
                        .takeIf { !it }?.let { finish() }
                }
            }
        })
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()

        when (val currentFragment = getCurrentFragment()) {
            is PlayerMobileFragment -> currentFragment.onUserLeaveHint()
        }
    }
}