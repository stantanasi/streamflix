package com.tanasi.streamflix.fragments.settings

import android.content.Intent
import android.os.Bundle
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory // Importa PreferenceCategory
import com.tanasi.streamflix.R
import com.tanasi.streamflix.providers.StreamingCommunityProvider
import com.tanasi.streamflix.utils.UserPreferences

class SettingsTvFragment : LeanbackPreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_tv, rootKey)

        displaySettings()
    }

    private fun displaySettings() {
        // Gestione visibilità categoria StreamingCommunity
        findPreference<PreferenceCategory>("pc_streamingcommunity_settings")?.apply {
            isVisible = UserPreferences.currentProvider is StreamingCommunityProvider
        }

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

                // La logica di riavvio quando il DoH cambia E il provider è StreamingCommunity
                // potrebbe non essere più necessaria qui se la categoria StreamingCommunity
                // è nascosta quando quel provider non è attivo.
                // Tuttavia, cambiare il DoH potrebbe comunque richiedere un riavvio
                // per applicare le modifiche a livello di rete per tutta l'app.
                // Valuta se questo blocco 'when' è ancora necessario o come dovrebbe comportarsi.
                // Per ora, lo lascio com'era, ma con un commento.
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

    // Potrebbe essere utile avere un metodo per aggiornare la visibilità
    // se il provider può cambiare mentre questa schermata è visibile.
    // Ad esempio, se l'utente torna a questa schermata dopo aver cambiato provider altrove.
    override fun onResume() {
        super.onResume()
        // Aggiorna la visibilità quando il fragment diventa visibile
        findPreference<PreferenceCategory>("pc_streamingcommunity_settings")?.isVisible =
            UserPreferences.currentProvider is StreamingCommunityProvider
    }
}
