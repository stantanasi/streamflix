package com.tanasi.streamflix.fragments.settings

import android.content.Intent
import android.os.Bundle
import android.util.Log // Aggiunto per il logging
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.tanasi.streamflix.R // Mantenuto per R.xml.settings_tv e altre preferenze
import com.tanasi.streamflix.providers.StreamingCommunityProvider
import com.tanasi.streamflix.utils.UserPreferences

class SettingsTvFragment : LeanbackPreferenceFragmentCompat() {

    companion object {
        private const val TAG = "SettingsTvFragment" // Tag per il logging
    }

    // Definizioni per i valori speciali
    private val DEFAULT_DOMAIN_VALUE = "streamingcommunityz.life"
    private val PREFS_ERROR_VALUE = "PREFS_NOT_INIT_ERROR" // Usato per la logica del testo nel dialogo

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_tv, rootKey)
        Log.d(TAG, "onCreatePreferences: Preferences set from XML for TV")
        displaySettings()
    }

    private fun displaySettings() {
        Log.d(TAG, "displaySettings START")
        val isStreamingCommunity = UserPreferences.currentProvider is StreamingCommunityProvider
        Log.d(TAG, "Current provider is StreamingCommunity: $isStreamingCommunity (Provider: ${UserPreferences.currentProvider?.name})")

        findPreference<PreferenceCategory>("pc_streamingcommunity_settings")?.apply {
            isVisible = isStreamingCommunity
            Log.d(TAG, "pc_streamingcommunity_settings visibility set to: $isVisible")
        }

        findPreference<EditTextPreference>("provider_streamingcommunity_domain")?.apply {
            val currentValue = UserPreferences.streamingcommunityDomain
            summary = currentValue
            Log.d(TAG, "provider_streamingcommunity_domain summary set to: $currentValue")

            if (currentValue == DEFAULT_DOMAIN_VALUE || currentValue == PREFS_ERROR_VALUE) {
                text = null
            } else {
                text = currentValue
            }
            Log.d(TAG, "provider_streamingcommunity_domain text set for dialog (null if default/error, else value)")


            setOnPreferenceChangeListener { preference, newValue ->
                val newDomainFromDialog = newValue as String
                UserPreferences.streamingcommunityDomain = newDomainFromDialog
                Log.d(TAG, "provider_streamingcommunity_domain changed to: $newDomainFromDialog")
                preference.summary = UserPreferences.streamingcommunityDomain
                Log.d(TAG, "provider_streamingcommunity_domain summary updated to: ${UserPreferences.streamingcommunityDomain}")

                if (UserPreferences.currentProvider is StreamingCommunityProvider) {
                    Log.d(TAG, "Rebuilding service and restarting activity for StreamingCommunityProvider due to domain change.")
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
                Log.d(TAG, "About preference clicked.")
                // TODO: Navigate to About screen for TV
                true
            }
        }

        findPreference<ListPreference>("p_doh_provider_url")?.apply {
            isVisible = isStreamingCommunity // Imposta visibilità iniziale
            Log.d(TAG, "p_doh_provider_url visibility set to: $isVisible")

            if (isVisible) { // Configura solo se visibile
                value = UserPreferences.dohProviderUrl ?: UserPreferences.DOH_DISABLED_VALUE
                summary = entry
                Log.d(TAG, "p_doh_provider_url value set to: $value, summary to: $entry")

                setOnPreferenceChangeListener { preference, newValue ->
                    val newUrl = newValue as String
                    UserPreferences.dohProviderUrl = newUrl
                    Log.d(TAG, "p_doh_provider_url changed to: $newUrl")

                    if (preference is ListPreference) {
                        val index = preference.findIndexOfValue(newUrl)
                        if (index >= 0 && preference.entries != null && index < preference.entries.size) {
                            preference.summary = preference.entries[index]
                            Log.d(TAG, "p_doh_provider_url summary updated to: ${preference.entries[index]}")
                        } else {
                            preference.summary = null
                            Log.d(TAG, "p_doh_provider_url summary set to null (index not found or entries null)")
                        }
                    }

                    if (UserPreferences.currentProvider is StreamingCommunityProvider) {
                        Log.d(TAG, "Rebuilding service and restarting activity for StreamingCommunityProvider due to DoH change.")
                        (UserPreferences.currentProvider as StreamingCommunityProvider).rebuildService()
                        requireActivity().apply {
                            finish()
                            startActivity(Intent(this, this::class.java))
                        }
                    }
                    true
                }
            } else {
                Log.d(TAG, "p_doh_provider_url is not visible, skipping configuration.")
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
            Log.d(TAG, "Network settings category title updated to: ${networkSettingsCategory.title}")
        } else {
            Log.w(TAG, "PreferenceCategory 'pc_network_settings' NOT FOUND.")
        }
        Log.d(TAG, "displaySettings END")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume START")
        val isStreamingCommunity = UserPreferences.currentProvider is StreamingCommunityProvider
        Log.d(TAG, "onResume: Current provider is StreamingCommunity: $isStreamingCommunity (Provider: ${UserPreferences.currentProvider?.name})")

        findPreference<PreferenceCategory>("pc_streamingcommunity_settings")?.isVisible =
            isStreamingCommunity
        Log.d(TAG, "onResume: pc_streamingcommunity_settings visibility updated to: $isStreamingCommunity")


        findPreference<EditTextPreference>("provider_streamingcommunity_domain")?.apply {
            val currentValue = UserPreferences.streamingcommunityDomain
            summary = currentValue
            Log.d(TAG, "onResume: provider_streamingcommunity_domain summary updated to: $currentValue")

            if (currentValue == DEFAULT_DOMAIN_VALUE || currentValue == PREFS_ERROR_VALUE) {
                text = null
            } else {
                text = currentValue
            }
            Log.d(TAG, "onResume: provider_streamingcommunity_domain text updated for dialog.")
        }

        findPreference<ListPreference>("p_doh_provider_url")?.apply {
            isVisible = isStreamingCommunity // Aggiorna visibilità
            Log.d(TAG, "onResume: p_doh_provider_url visibility updated to: $isVisible")
            if (isVisible) { // Aggiorna sommario solo se visibile
                val currentValue = UserPreferences.dohProviderUrl ?: UserPreferences.DOH_DISABLED_VALUE
                val currentIndex = findIndexOfValue(currentValue)
                if (currentIndex >= 0 && entries != null && currentIndex < entries.size) {
                    summary = entries[currentIndex]
                    Log.d(TAG, "onResume: p_doh_provider_url summary updated to: ${entries[currentIndex]}")
                } else {
                    summary = null // O un testo di default se preferisci
                    Log.d(TAG, "onResume: p_doh_provider_url summary set to null (value not in entries or entries null)")
                }
            } else {
                summary = null // Assicurati che il sommario sia vuoto se la preferenza non è visibile
                Log.d(TAG, "onResume: p_doh_provider_url is not visible, summary cleared.")
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
            Log.d(TAG, "onResume: Network settings category title updated to: ${networkSettingsCategory.title}")
        } else {
            Log.w(TAG, "onResume: PreferenceCategory 'pc_network_settings' NOT FOUND.")
        }
        Log.d(TAG, "onResume END")
    }
}

