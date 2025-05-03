package com.tanasi.streamflix.fragments.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.tanasi.streamflix.R
import com.tanasi.streamflix.utils.UserPreferences
import kotlin.system.exitProcess

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

        findPreference<Preference>("p_settings_help")?.apply {
            setOnPreferenceClickListener {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/stantanasi/streamflix")
                    )
                )
                true
            }
        }

        findPreference<Preference>("p_settings_close_app")?.apply {
            setOnPreferenceClickListener {
                exitProcess(0)
                true
            }
        }

        findPreference<Preference>("p_settings_streamingcommunity_domain")?.apply {
            setDefaultValue(UserPreferences.streamingcommunityDomain)

            setOnPreferenceChangeListener { _, newValue ->
                UserPreferences.streamingcommunityDomain = newValue as String
                true
            }
        }
    }
}