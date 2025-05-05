package com.tanasi.streamflix.fragments.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.widget.Toast
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
            summary = UserPreferences.streamingcommunityDomain

            setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_TEXT
                editText.imeOptions = EditorInfo.IME_ACTION_DONE

                editText.hint = "streamingcommunity.spa"

                val pref = UserPreferences.streamingcommunityDomain
                if (pref.isNullOrEmpty())
                    editText.setText("streamingcommunity.spa")
                else
                    editText.setText(pref)
            }

            setOnPreferenceChangeListener { _, newValue ->
                UserPreferences.streamingcommunityDomain = newValue as String
                summary = newValue

                Toast.makeText(
                    requireContext(),
                    getString(R.string.settings_streamingcommunity_close_app),
                    Toast.LENGTH_LONG
                ).show()
                Handler(Looper.getMainLooper()).postDelayed({
                    exitProcess(0)
                }, 3000)

                true
            }
        }
    }
}