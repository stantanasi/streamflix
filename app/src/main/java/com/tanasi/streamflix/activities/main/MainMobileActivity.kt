package com.tanasi.streamflix.activities.main

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.tanasi.streamflix.R
import com.tanasi.streamflix.databinding.ActivityMainMobileBinding

class MainMobileActivity : FragmentActivity() {

    private var _binding: ActivityMainMobileBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_Base)
        super.onCreate(savedInstanceState)
        _binding = ActivityMainMobileBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}