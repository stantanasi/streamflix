package com.tanasi.streamflix.fragments.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.inputmethod.EditorInfo
import androidx.navigation.fragment.findNavController
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.tanasi.streamflix.R
import com.tanasi.streamflix.providers.StreamingCommunityProvider
import com.tanasi.streamflix.utils.UserPreferences

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

        findPreference<EditTextPreference>("p_settings_streamingcommunity_domain")?.apply {
            summary = UserPreferences.streamingcommunityDomain

            setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_TEXT
                editText.imeOptions = EditorInfo.IME_ACTION_DONE

                editText.hint = "streamingcommunity.example"

                val pref = UserPreferences.streamingcommunityDomain
                if (pref.isNullOrEmpty())
                    editText.setText("streamingcommunity.example")
                else
                    editText.setText(pref)
            }

            setOnPreferenceChangeListener { _, newValue ->
                UserPreferences.streamingcommunityDomain = newValue as String
                summary = newValue

                when (UserPreferences.currentProvider) {
                    is StreamingCommunityProvider -> {
//                        re build service
                        (UserPreferences.currentProvider as StreamingCommunityProvider).rebuildService(newValue)

//                        restart activity
                        requireActivity().apply {
                            finish()
                            startActivity(Intent(this, this::class.java))
                        }
                    }
                }

                true
            }
        }

        findPreference<SwitchPreference>("p_settings_streamingcommunity_dnsOverHttps")?.apply {
            isChecked = UserPreferences.streamingcommunityDnsOverHttps

            setOnPreferenceChangeListener { _, newValue ->
                UserPreferences.streamingcommunityDnsOverHttps = newValue as Boolean

                when (UserPreferences.currentProvider) {
                    is StreamingCommunityProvider -> {
                        (UserPreferences.currentProvider as StreamingCommunityProvider).rebuildService()

                        requireActivity().apply {
                            finish()
                            startActivity(Intent(this, this::class.java))
                        }
                    }
                }

                true
            }
        }
    }
}