package com.tanasi.streamflix.fragments.settings.mobile

import android.os.Bundle
import android.util.Log
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import com.tanasi.streamflix.R
import com.tanasi.streamflix.utils.UserPreferences

class SettingsMobileFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        Log.d("SettingsDebug", "SettingsMobileFragment: onCreatePreferences CALLED")
        setPreferencesFromResource(R.xml.settings_mobile, rootKey)

        val streamingCommunityDomainPreference = findPreference<EditTextPreference>("provider_streamingcommunity_domain")

        if (streamingCommunityDomainPreference == null) {
            Log.e("SettingsDebug", "SettingsMobileFragment: provider_streamingcommunity_domain preference NOT FOUND with key 'provider_streamingcommunity_domain'!")
        } else {
            Log.d("SettingsDebug", "SettingsMobileFragment: provider_streamingcommunity_domain preference FOUND.")

            // Imposta il sommario iniziale basato su UserPreferences
            val initialUserPrefValue = UserPreferences.streamingcommunityDomain // Corretto qui
            streamingCommunityDomainPreference.summary = initialUserPrefValue
            Log.d("SettingsDebug", "Initial summary set to UserPreferences value: '$initialUserPrefValue'")

            streamingCommunityDomainPreference.setOnBindEditTextListener { editText ->
                Log.d("SettingsDebug", "setOnBindEditTextListener CALLED.")
                val currentValueFromUserPrefs = UserPreferences.streamingcommunityDomain // Corretto qui
                Log.d("SettingsDebug", "setOnBindEditTextListener: Current value from UserPreferences.streamingcommunityDomain is: '$currentValueFromUserPrefs'")
                Log.d("SettingsDebug", "setOnBindEditTextListener: EditText text before setting: '${editText.text}'")

                editText.setText(currentValueFromUserPrefs)
                // Controlla se l'hint è presente e se il testo è vuoto, l'hint verrà mostrato
                if (currentValueFromUserPrefs.isEmpty() && editText.hint != null) {
                    Log.d("SettingsDebug", "setOnBindEditTextListener: Setting empty text, EditText should show hint: '${editText.hint}'")
                }
                editText.setSelection(currentValueFromUserPrefs.length)
                Log.d("SettingsDebug", "setOnBindEditTextListener: EditText text AFTER setting: '${editText.text}'")
            }

            streamingCommunityDomainPreference.setOnPreferenceChangeListener { preference, newValue ->
                Log.d("SettingsDebug", "setOnPreferenceChangeListener CALLED. Preference key: '${preference.key}', New value: '$newValue'")
                val newDomain = newValue as String

                if (newDomain.isNotEmpty()) {
                    UserPreferences.streamingcommunityDomain = newDomain // Salva in UserPreferences - Corretto qui
                    preference.summary = newDomain // Aggiorna il sommario della preferenza UI
                    Log.d("SettingsDebug", "setOnPreferenceChangeListener: Domain saved in UserPreferences ('${UserPreferences.streamingcommunityDomain}') and summary updated to '$newDomain'.") // Corretto qui
                } else {
                    // Se il dominio è vuoto, salva una stringa vuota e aggiorna il sommario di conseguenza.
                    UserPreferences.streamingcommunityDomain = "" // Corretto qui
                    preference.summary = "Nessun dominio impostato (default)" // O mostra l'hint o un testo appropriato
                    Log.d("SettingsDebug", "setOnPreferenceChangeListener: Domain is empty. Saved as empty in UserPreferences. Summary updated.")
                }
                true // Restituisci true per persistere il valore in SharedPreferences tramite EditTextPreference
            }
            Log.d("SettingsDebug", "SettingsMobileFragment: Listeners SET for provider_streamingcommunity_domain.")
        }
    }
}
