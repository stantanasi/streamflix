package com.tanasi.sflix.activities

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.tanasi.sflix.R
import com.tanasi.sflix.fragments.home.HomeFragment

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_browse_fragment, HomeFragment())
                .commitNow()
        }
    }
}