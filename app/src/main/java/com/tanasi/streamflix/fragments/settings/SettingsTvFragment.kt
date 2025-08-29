package com.tanasi.streamflix.fragments.settings

import android.content.Intent
import android.os.Bundle
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import androidx.preference.PreferenceCategory
import com.tanasi.streamflix.R // Mantenuto per R.xml.settings_tv e altre preferenze
import com.tanasi.streamflix.providers.StreamingCommunityProvider
import com.tanasi.streamflix.utils.UserPreferences

class SettingsTvFragment : LeanbackPreferenceFragmentCompat() {

    // Definizioni per i valori speciali
    private val DEFAULT_DOMAIN_VALUE = "streamingcommunityz.life"
    private val PREFS_ERROR_VALUE = "PREFS_NOT_INIT_ERROR" // Usato per la logica del testo nel dialogo

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_tv, rootKey)
        displaySettings()
    }

    private fun displaySettings() {
        findPreference<PreferenceCategory>("pc_streamingcommunity_settings")?.apply {
            isVisible = UserPreferences.currentProvider is StreamingCommunityProvider
        }

        findPreference<EditTextPreference>("provider_streamingcommunity_domain")?.apply {
            val currentValue = UserPreferences.streamingcommunityDomain

            // Imposta il sommario per mostrare sempre il valore corrente effettivo
            summary = currentValue

            // Logica per il testo nel dialogo EditTextPreference
            // Lascia vuoto se è il default o errore (per mostrare l'hint'),
            // altrimenti pre-compila con il dominio personalizzato.
            if (currentValue == DEFAULT_DOMAIN_VALUE || currentValue == PREFS_ERROR_VALUE) {
                text = null
            } else {
                text = currentValue
            }

            setOnPreferenceChangeListener { preference, newValue ->
                val newDomainFromDialog = newValue as String
                // Aggiorna il valore nelle UserPreferences.
                // Il setter in UserPreferences gestirà il caso di stringa vuota,
                // reimpostando al valore di default.
                UserPreferences.streamingcommunityDomain = newDomainFromDialog

                // Aggiorna il sommario della preferenza per riflettere il valore effettivo
                // (che potrebbe essere il default se newDomainFromDialog era vuoto).
                preference.summary = UserPreferences.streamingcommunityDomain

                // Logica di riavvio/ricostruzione del servizio
                if (UserPreferences.currentProvider is StreamingCommunityProvider) {
                    (UserPreferences.currentProvider as StreamingCommunityProvider).rebuildService()
                    requireActivity().apply {
                        finish()
                        startActivity(Intent(this, this::class.java))
                    }
                }
                true
            }
        }

        findPreference<Preference>("p_settings_about")?.apply {
            setOnPreferenceClickListener {
                // TODO: Navigate to About screen for TV
                true
            }
        }

        findPreference<SwitchPreference>("AUTOPLAY")?.apply {
            isChecked = UserPreferences.autoplay
            setOnPreferenceChangeListener { _, newValue ->
                UserPreferences.autoplay = newValue as Boolean
                true
            }
        }

        findPreference<ListPreference>("p_doh_provider_url")?.apply {
            value = UserPreferences.dohProviderUrl // Modificato: operatore Elvis rimosso
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

                // Logica di riavvio/ricostruzione del servizio per DoH
                if (UserPreferences.currentProvider is StreamingCommunityProvider) {
                    (UserPreferences.currentProvider as StreamingCommunityProvider).rebuildService()
                    requireActivity().apply {
                        finish()
                        startActivity(Intent(this, this::class.java))
                    }
                }
                true
            }
        }

        val networkSettingsCategory = findPreference<PreferenceCategory>("pc_network_settings")
        if (networkSettingsCategory != null) {
            val originalTitle = getString(R.string.settings_category_network_title)
            val currentProviderName = UserPreferences.currentProvider?.name
            if (currentProviderName != null && currentProviderName.isNotEmpty()) {
                networkSettingsCategory.title = "$originalTitle $currentProviderName"
            } else {
                networkSettingsCategory.title = originalTitle
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Aggiorna la visibilità della categoria StreamingCommunity
        findPreference<PreferenceCategory>("pc_streamingcommunity_settings")?.isVisible =
            UserPreferences.currentProvider is StreamingCommunityProvider

        // Aggiorna il sommario e il testo del dialogo per il dominio StreamingCommunity
        findPreference<EditTextPreference>("provider_streamingcommunity_domain")?.apply {
            val currentValue = UserPreferences.streamingcommunityDomain
            // Imposta il sommario per mostrare sempre il valore corrente effettivo
            summary = currentValue

            // Logica per il testo nel dialogo EditTextPreference
            if (currentValue == DEFAULT_DOMAIN_VALUE || currentValue == PREFS_ERROR_VALUE) {
                text = null
            } else {
                text = currentValue
            }
        }

        // Aggiorna visibilità e sommario DoH in onResume
        findPreference<ListPreference>("p_doh_provider_url")?.apply {
            summary = entry
        }

        val networkSettingsCategory = findPreference<PreferenceCategory>("pc_network_settings")
        if (networkSettingsCategory != null) {
            val originalTitle = getString(R.string.settings_category_network_title)
            val currentProviderName = UserPreferences.currentProvider?.name
            if (currentProviderName != null && currentProviderName.isNotEmpty()) {
                networkSettingsCategory.title = "$originalTitle $currentProviderName"
            } else {
                networkSettingsCategory.title = originalTitle
            }
        }
        findPreference<SwitchPreference>("AUTOPLAY")?.isChecked = UserPreferences.autoplay
        val bufferPref: EditTextPreference? = findPreference("p_settings_autoplay_buffer")
        bufferPref?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { pref ->
            val value = pref.text?.toLongOrNull() ?: 3L
            "$value s"
        }
    }
}
