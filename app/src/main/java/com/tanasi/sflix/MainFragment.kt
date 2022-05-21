package com.tanasi.sflix

import android.os.Bundle
import androidx.leanback.app.BrowseSupportFragment

class MainFragment : BrowseSupportFragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        title = getString(R.string.app_name)
    }
}