package com.tanasi.streamflix.activities.main

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.tanasi.streamflix.R
import com.tanasi.streamflix.database.AppDatabase
import com.tanasi.streamflix.databinding.ActivityMainMobileBinding
import com.tanasi.streamflix.ui.UpdateMobileDialog
import com.tanasi.streamflix.utils.UserPreferences

class MainMobileActivity : FragmentActivity() {

    private var _binding: ActivityMainMobileBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<MainViewModel>()

    private lateinit var updateDialog: UpdateMobileDialog

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
        UserPreferences.currentProvider?.let {
            navController.navigate(R.id.home)
        }

        viewModel.checkUpdate()

        binding.bnvMain.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.search,
                R.id.home,
                R.id.movies,
                R.id.tv_shows -> binding.bnvMain.visibility = View.VISIBLE
                else -> binding.bnvMain.visibility = View.GONE
            }
        }

        viewModel.state.observe(this) { state ->
            when (state) {
                MainViewModel.State.CheckingUpdate -> {}
                is MainViewModel.State.SuccessCheckingUpdate -> {
                    val asset = state.release?.assets
                        ?.find { it.contentType == "application/vnd.android.package-archive" }
                    if (asset != null) {
                        updateDialog = UpdateMobileDialog(this).also {
                            it.release = state.release
                            it.setOnUpdateClickListener { _ ->
                                if (!it.isLoading) viewModel.downloadUpdate(this, asset)
                            }
                            it.show()
                        }
                    }
                }

                MainViewModel.State.DownloadingUpdate -> updateDialog.isLoading = true
                is MainViewModel.State.SuccessDownloadingUpdate -> {
                    viewModel.installUpdate(this, state.apk)
                    updateDialog.hide()
                }

                MainViewModel.State.InstallingUpdate -> updateDialog.isLoading = true

                is MainViewModel.State.FailedUpdate -> {
                    Toast.makeText(
                        this,
                        state.error.message ?: "",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}