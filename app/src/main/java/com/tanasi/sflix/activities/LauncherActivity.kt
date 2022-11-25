package com.tanasi.sflix.activities

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.tanasi.sflix.R
import com.tanasi.sflix.databinding.ActivityLauncherBinding

class LauncherActivity : FragmentActivity() {

    private var _binding: ActivityLauncherBinding? = null
    private val binding: ActivityLauncherBinding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_SFlix)
        super.onCreate(savedInstanceState)
        _binding = ActivityLauncherBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}