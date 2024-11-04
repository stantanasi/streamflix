package com.tanasi.streamflix.fragments.settings.about

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.tanasi.streamflix.BuildConfig
import com.tanasi.streamflix.R

class SettingsAboutMobileFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_about_mobile, rootKey)

        displaySettingsAbout()
    }

    private fun displaySettingsAbout() {
        findPreference<Preference>("p_settings_about_version")?.apply {
            summary = getString(R.string.settings_about_version_name, BuildConfig.VERSION_NAME)
        }
    }
}