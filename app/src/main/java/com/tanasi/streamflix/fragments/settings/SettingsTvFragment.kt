package com.tanasi.streamflix.fragments.settings

import android.content.Intent // Import necessario per Intent
import android.os.Bundle
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.tanasi.streamflix.R
import com.tanasi.streamflix.providers.StreamingCommunityProvider
import com.tanasi.streamflix.utils.UserPreferences
// Rimuovi o commenta: import com.tanasi.streamflix.utils.restartApp

class SettingsTvFragment : LeanbackPreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_tv, rootKey)

        displaySettings()
    }

    private fun displaySettings() {
        findPreference<Preference>("p_settings_about")?.apply {
            setOnPreferenceClickListener {
                // TODO: Navigate to About screen for TV
                true
            }
        }

        findPreference<ListPreference>("p_doh_provider_url")?.apply {
            value = UserPreferences.dohProviderUrl ?: UserPreferences.DOH_DISABLED_VALUE
            summary = entry // Imposta il sommario iniziale

            setOnPreferenceChangeListener { preference, newValue ->
                val newUrl = newValue as String
                UserPreferences.dohProviderUrl = newUrl

                if (preference is ListPreference) {
                    val index = preference.findIndexOfValue(newUrl)
                    if (index >= 0 && preference.entries != null && index < preference.entries.size) {
                        preference.summary = preference.entries[index]
                    } else {
                        preference.summary = null // o una stringa di default
                    }
                }

                when (UserPreferences.currentProvider) {
                    is StreamingCommunityProvider -> {
                        (UserPreferences.currentProvider as StreamingCommunityProvider).rebuildService()
                        // Usa la stessa logica di riavvio del mobile
                        requireActivity().apply {
                            finish()
                            startActivity(Intent(this, this::class.java)) // Riga chiave per il riavvio
                        }
                    }
                }
                true
            }
        } // Chiusura del blocco apply per p_doh_provider_url
    } // Chiusura del metodo displaySettings() <--- PARENTESI AGGIUNTA
} // Chiusura della classe SettingsTvFragment <--- PARENTESI AGGIUNTA
