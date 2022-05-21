package com.tanasi.sflix.fragments.home

import android.os.Bundle
import androidx.leanback.app.BrowseSupportFragment
import com.tanasi.sflix.R

class HomeFragment : BrowseSupportFragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        title = getString(R.string.app_name)
    }
}