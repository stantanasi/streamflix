package com.tanasi.streamflix.fragments.settings.mobile

import android.os.Bundle
import android.util.Log
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.tanasi.streamflix.R
import com.tanasi.streamflix.providers.StreamingCommunityProvider // Assicurati che l'import sia corretto
import com.tanasi.streamflix.utils.UserPreferences

class SettingsMobileFragment : PreferenceFragmentCompat() {

    companion object {
        private const val TAG = "SettingsMobileFragment" // Tag per i log specifici di questo fragment
        private const val DEBUG_PROVIDER_TAG = "SettingsMobileDebug" // Tag per i log di debug del provider
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_mobile, rootKey)
        Log.d(TAG, "onCreatePreferences: Preferences set from XML")

        setupStreamingCommunityDomainPreference()
        updateNetworkSettingsTitle()
        updateStreamingCommunityCategoryVisibility()
        setupDohPreference()
    }

    override fun onResume() {
        super.onResume()
        Log.d(DEBUG_PROVIDER_TAG, "onResume START")
        Log.d(DEBUG_PROVIDER_TAG, "Current provider name from UserPreferences: ${UserPreferences.currentProvider?.name}")
        Log.d(DEBUG_PROVIDER_TAG, "Current provider class from UserPreferences: ${UserPreferences.currentProvider?.javaClass?.name}")
        val isStreamingCommunity = UserPreferences.currentProvider is StreamingCommunityProvider
        Log.d(DEBUG_PROVIDER_TAG, "Is StreamingCommunityProvider active? $isStreamingCommunity")

        updateNetworkSettingsTitle()
        updateStreamingCommunityCategoryVisibility() // Assicura che la visibilità sia aggiornata
        updateSummaries() // Aggiorna i sommari delle preferenze che potrebbero cambiare
    }

    private fun setupStreamingCommunityDomainPreference() {
        val domainPreference = findPreference<EditTextPreference>("provider_streamingcommunity_domain")
        if (domainPreference == null) {
            Log.e(TAG, "Preference 'provider_streamingcommunity_domain' NOT FOUND.")
            return
        }

        domainPreference.summary = UserPreferences.streamingcommunityDomain ?: "Nessun dominio impostato"

        domainPreference.setOnBindEditTextListener { editText ->
            val currentValue = UserPreferences.streamingcommunityDomain
            editText.setText(currentValue)
            editText.setSelection(currentValue.length)
            Log.d(TAG, "setOnBindEditTextListener: EditText bound with value: $currentValue")
        }

        domainPreference.setOnPreferenceChangeListener { preference, newValue ->
            val newDomain = newValue as? String ?: ""
            UserPreferences.streamingcommunityDomain = newDomain
            preference.summary = if (newDomain.isNotEmpty()) newDomain else "Nessun dominio impostato (default)"
            Log.d(TAG, "setOnPreferenceChangeListener: Domain set to '$newDomain', summary updated.")
            true
        }
    }

    private fun updateNetworkSettingsTitle() {
        val networkSettingsCategory = findPreference<PreferenceCategory>("pc_network_settings")
        if (networkSettingsCategory == null) {
            Log.e(TAG, "PreferenceCategory 'pc_network_settings' NOT FOUND.")
            return
        }

        try {
            val originalTitle = getString(R.string.settings_category_network_title)
            val currentProviderName = UserPreferences.currentProvider?.name
            if (!currentProviderName.isNullOrEmpty()) {
                networkSettingsCategory.title = "$originalTitle $currentProviderName"
                Log.d(TAG, "Network settings title updated to include provider: $currentProviderName")
            } else {
                networkSettingsCategory.title = originalTitle
                Log.d(TAG, "Network settings title set to original (no provider name).")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting string R.string.settings_category_network_title. Make sure it exists.", e)
            networkSettingsCategory.title = "Network Settings" // Fallback title
        }
    }

    private fun updateStreamingCommunityCategoryVisibility() {
        val streamingCommunitySettingsCategory = findPreference<PreferenceCategory>("pc_streamingcommunity_settings")
        if (streamingCommunitySettingsCategory == null) {
            Log.e(TAG, "PreferenceCategory 'pc_streamingcommunity_settings' NOT FOUND.")
            return
        }
        val isVisible = UserPreferences.currentProvider is StreamingCommunityProvider
        streamingCommunitySettingsCategory.isVisible = isVisible
        Log.d(TAG, "StreamingCommunity settings category visibility set to: $isVisible")
    }

    private fun setupDohPreference() {
        val dohPreference = findPreference<ListPreference>("p_doh_provider_url")
        if (dohPreference == null) {
            Log.d(TAG, "ListPreference 'p_doh_provider_url' NOT FOUND. Skipping DOH setup.")
            return
        }

        dohPreference.value = UserPreferences.dohProviderUrl ?: UserPreferences.DOH_DISABLED_VALUE // Assumendo che DOH_DISABLED_VALUE esista in UserPreferences
        dohPreference.summary = dohPreference.entry // Imposta sommario iniziale

        dohPreference.setOnPreferenceChangeListener { preference, newValue ->
            val newUrl = newValue as String
            UserPreferences.dohProviderUrl = newUrl

            // Aggiorna il sommario per riflettere la nuova selezione
            val listPreference = preference as ListPreference
            val index = listPreference.findIndexOfValue(newUrl)
            if (index >= 0) {
                listPreference.summary = listPreference.entries[index]
            } else {
                listPreference.summary = null // O un sommario di default se il valore non è trovato
            }
            Log.d(TAG, "DOH provider URL set to: $newUrl, summary updated.")
            true
        }
    }

    private fun updateSummaries() {
        // Aggiorna il sommario per il dominio StreamingCommunity
        val domainPreference = findPreference<EditTextPreference>("provider_streamingcommunity_domain")
        domainPreference?.summary = UserPreferences.streamingcommunityDomain ?: "Nessun dominio impostato"

        // Aggiorna il sommario per DoH
        val dohPreference = findPreference<ListPreference>("p_doh_provider_url")
        if (dohPreference != null) {
            val currentValue = UserPreferences.dohProviderUrl ?: UserPreferences.DOH_DISABLED_VALUE
            val currentIndex = dohPreference.findIndexOfValue(currentValue)
            if (currentIndex >= 0) {
                dohPreference.summary = dohPreference.entries[currentIndex]
            } else {
                 // Potrebbe essere necessario cercare il valore di default o un testo placeholder
                dohPreference.summary = "Seleziona provider DoH"
            }
        }
        Log.d(TAG, "Summaries updated in onResume.")
    }
}
