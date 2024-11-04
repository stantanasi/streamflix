package com.tanasi.streamflix.fragments.settings

import android.os.Bundle
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.tanasi.streamflix.R

class SettingsMobileFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_mobile, rootKey)

        displaySettings()
    }

    private fun displaySettings() {
        findPreference<Preference>("p_settings_about")?.apply {
            setOnPreferenceClickListener {
                findNavController().navigate(
                    SettingsMobileFragmentDirections.actionSettingsToSettingsAbout()
                )
                true
            }
        }
    }
}