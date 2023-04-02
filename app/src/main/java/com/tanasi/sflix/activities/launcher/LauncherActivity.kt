package com.tanasi.sflix.activities.launcher

import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.tanasi.sflix.R
import com.tanasi.sflix.databinding.ActivityLauncherBinding
import com.tanasi.sflix.utils.AppPreferences

class LauncherActivity : FragmentActivity() {

    private var _binding: ActivityLauncherBinding? = null
    private val binding: ActivityLauncherBinding get() = _binding!!

    private val viewModel by viewModels<LauncherViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_SFlix)
        super.onCreate(savedInstanceState)
        _binding = ActivityLauncherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AppPreferences.setup(this)
    }
}