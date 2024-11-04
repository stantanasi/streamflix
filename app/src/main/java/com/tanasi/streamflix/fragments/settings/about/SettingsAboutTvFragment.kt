package com.tanasi.streamflix.fragments.settings.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat
import androidx.preference.Preference
import com.tanasi.streamflix.BuildConfig
import com.tanasi.streamflix.R

class SettingsAboutTvFragment : LeanbackPreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_about_tv, rootKey)

        displaySettingsAbout()
    }

    private fun displaySettingsAbout() {
        findPreference<Preference>("p_settings_about_version")?.apply {
            summary = getString(R.string.settings_about_version_name, BuildConfig.VERSION_NAME)
        }

        findPreference<Preference>("p_settings_about_donate")?.apply {
            setOnPreferenceClickListener {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.buymeacoffee.com/stantanasi")
                    )
                )
                true
            }
        }
    }
}