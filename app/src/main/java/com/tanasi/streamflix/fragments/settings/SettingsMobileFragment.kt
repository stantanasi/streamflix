package com.tanasi.streamflix.fragments.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.inputmethod.EditorInfo
import androidx.navigation.fragment.findNavController
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory // Importa PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.tanasi.streamflix.R
//import com.tanasi.streamflix.fragments.settings.mobile.SettingsMobileFragmentDirections
import com.tanasi.streamflix.providers.StreamingCommunityProvider
import com.tanasi.streamflix.utils.UserPreferences

class SettingsMobileFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_mobile, rootKey)

        displaySettings()
    }

    private fun displaySettings() {
        // Gestione visibilità categoria StreamingCommunity
        findPreference<PreferenceCategory>("pc_streamingcommunity_settings")?.apply {
            isVisible = UserPreferences.currentProvider is StreamingCommunityProvider
        }
        findPreference<SwitchPreference>("AUTOPLAY")?.apply {
            isChecked = UserPreferences.autoplay
            setOnPreferenceChangeListener { _, newValue ->
                UserPreferences.autoplay = newValue as Boolean
                true
            }
        }

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

                editText.hint = "streamingcommunity.example" // Potrebbe essere una stringa di risorsa

                // Mantieni la logica per pre-popolare il testo se necessario
                val pref = UserPreferences.streamingcommunityDomain
                if (pref.isNullOrEmpty()) {
                    // Considera se vuoi impostare un testo di default o lasciare l'hint
                    // editText.setText("streamingcommunity.to") // Esempio se vuoi un default
                } else {
                    editText.setText(pref)
                }
            }

            setOnPreferenceChangeListener { _, newValue ->
                val newDomain = newValue as String
                UserPreferences.streamingcommunityDomain = newDomain
                summary = newDomain

                // Se il provider corrente è StreamingCommunity, ricostruisci il servizio e riavvia
                if (UserPreferences.currentProvider is StreamingCommunityProvider) {
                    (UserPreferences.currentProvider as StreamingCommunityProvider).rebuildService(newDomain)
                    requireActivity().apply {
                        finish()
                        startActivity(Intent(this, this::class.java))
                    }
                }
                true
            }
        }

        findPreference<ListPreference>("p_doh_provider_url")?.apply {
            value = UserPreferences.dohProviderUrl ?: UserPreferences.DOH_DISABLED_VALUE
            summary = entry

            setOnPreferenceChangeListener { preference, newValue ->
                val newUrl = newValue as String
                UserPreferences.dohProviderUrl = newUrl

                if (preference is ListPreference) {
                    val index = preference.findIndexOfValue(newUrl)
                    if (index >= 0 && preference.entries != null && index < preference.entries.size) {
                        preference.summary = preference.entries[index]
                    } else {
                        preference.summary = null
                    }
                }

                // Logica di riavvio: valuta se è sempre necessaria dopo cambio DoH
                // o solo se StreamingCommunity è attivo (come è ora).
                // Per coerenza con SettingsTvFragment e il commento precedente,
                // lascio la logica condizionale, ma potrebbe essere generalizzata.
                if (UserPreferences.currentProvider is StreamingCommunityProvider) {
                    (UserPreferences.currentProvider as StreamingCommunityProvider).rebuildService() // Usa il dominio da UserPreferences
                    requireActivity().apply {
                        finish()
                        startActivity(Intent(this, this::class.java))
                    }
                }
                true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Aggiorna la visibilità quando il fragment diventa visibile
        findPreference<PreferenceCategory>("pc_streamingcommunity_settings")?.isVisible =
            UserPreferences.currentProvider is StreamingCommunityProvider
        findPreference<SwitchPreference>("AUTOPLAY")?.isChecked = UserPreferences.autoplay
        val bufferPref: EditTextPreference? = findPreference("p_settings_autoplay_buffer")
        bufferPref?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { pref ->
            val value = pref.text?.toLongOrNull() ?: 3L
            "$value seconds"
        }
    }
}
