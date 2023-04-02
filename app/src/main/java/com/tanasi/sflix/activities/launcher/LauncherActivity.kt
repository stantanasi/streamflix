package com.tanasi.sflix.activities.launcher

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.tanasi.sflix.R
import com.tanasi.sflix.databinding.ActivityLauncherBinding
import com.tanasi.sflix.ui.UpdateDialog
import com.tanasi.sflix.utils.AppPreferences

class LauncherActivity : FragmentActivity() {

    private var _binding: ActivityLauncherBinding? = null
    private val binding: ActivityLauncherBinding get() = _binding!!

    private val viewModel by viewModels<LauncherViewModel>()

    private lateinit var updateDialog: UpdateDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_SFlix)
        super.onCreate(savedInstanceState)
        _binding = ActivityLauncherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AppPreferences.setup(this)

        viewModel.state.observe(this) { state ->
            when (state) {
                LauncherViewModel.State.CheckingUpdate -> {}
                is LauncherViewModel.State.SuccessCheckingUpdate -> {
                    val asset = state.release?.assets
                        ?.find { it.contentType == "application/vnd.android.package-archive" }
                    if (asset != null) {
                        updateDialog = UpdateDialog(this).also {
                            it.release = state.release
                            it.show()
                        }
                    }
                }

                is LauncherViewModel.State.FailedUpdate -> {
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