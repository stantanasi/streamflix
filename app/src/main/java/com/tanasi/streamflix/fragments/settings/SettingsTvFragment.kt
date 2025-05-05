package com.tanasi.streamflix.fragments.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat
import androidx.navigation.fragment.findNavController
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import com.tanasi.streamflix.R
import com.tanasi.streamflix.utils.UserPreferences
import kotlin.system.exitProcess

class SettingsTvFragment : LeanbackPreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_tv, rootKey)

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

        findPreference<EditTextPreference>("p_settings_streamingcommunity_domain")?.apply {
            setDefaultValue(UserPreferences.streamingcommunityDomain)

            setOnBindEditTextListener { editText ->
                editText.hint = "streamingcommunity.spa"
            }

            setOnPreferenceChangeListener { _, newValue ->
                UserPreferences.streamingcommunityDomain = newValue as String
                requireActivity().apply {
                    finish()
                    startActivity(Intent(this, this::class.java))
                }
                true
            }
        }
    }
}