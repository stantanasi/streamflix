package com.tanasi.streamflix.fragments.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.tanasi.streamflix.R

class SettingsMobileFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_mobile, rootKey)
    }
}