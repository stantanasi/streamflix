package com.tanasi.streamflix.fragments.settings.about

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.tanasi.streamflix.R

class SettingsAboutMobileFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_about_mobile, rootKey)
    }
}