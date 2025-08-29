package com.tanasi.streamflix.utils

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.tanasi.streamflix.R
import java.util.*

class VoiceRecognitionHelper(
    private val fragment: Fragment,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onListeningStateChanged: (Boolean) -> Unit
) {

    private val context = fragment.requireContext()
    private var speechRecognizer: SpeechRecognizer? = null

    var isListening: Boolean = false
        private set

    private val permissionLauncher =
        fragment.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) startRecognition() else {
                onError(context.getString(R.string.voice_error_permission_denied))
            }
        }

    fun startWithPermissionCheck() {
        val permission = Manifest.permission.RECORD_AUDIO
        when {
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED -> {
                startRecognition()
            }

            fragment.shouldShowRequestPermissionRationale(permission) -> {
                AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.voice_permission_title))
                    .setMessage(context.getString(R.string.voice_permission_rationale))
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        permissionLauncher.launch(permission)
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        onError(context.getString(R.string.voice_error_permission_denied))
                    }
                    .show()
            }

            else -> permissionLauncher.launch(permission)
        }
    }

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    private fun startRecognition() {
        if (isListening) {
            stopRecognition()
        }


        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    val activeLang = params?.getString("language")
                    Log.d("VoiceRecognitionHelper", "Reconocimiento activo en: $activeLang")
                    onListeningStateChanged(true)
                    isListening = true
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    onListeningStateChanged(false)
                    isListening = false
                }

                override fun onError(error: Int) {
                    onListeningStateChanged(false)
                    isListening = false

                    val msg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> fragment.getString(R.string.voice_error_no_match)
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> fragment.getString(R.string.voice_error_timeout)
                        SpeechRecognizer.ERROR_AUDIO -> fragment.getString(R.string.voice_error_audio)
                        else -> fragment.getString(R.string.voice_error_generic)
                    }
                    onError(msg)


                }

                override fun onResults(results: Bundle?) {
                    onListeningStateChanged(false)
                    isListening = false

                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val query = matches?.firstOrNull()?.trim()
                    if (!query.isNullOrEmpty()) {
                        onResult(query)
                    } else {
                        onError(context.getString(R.string.voice_error_no_result))
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val providerLanguage: String = UserPreferences.currentProvider
            ?.language?:""

        val languageTag = resolveFullLanguageTag(providerLanguage);

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            if (VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // 34
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_DETECTION_ALLOWED_LANGUAGES, true)
            }
        }


        speechRecognizer?.startListening(intent)
    }

    fun stopRecognition() {
        speechRecognizer?.apply {
            stopListening()
            cancel()
            destroy()
        }
        speechRecognizer = null
        isListening = false
        onListeningStateChanged(false)

    }

    fun resolveFullLanguageTag(partialTag: String): String {
        val supportedTags = mapOf(
            "en" to "en-US",
            "es" to "es-ES",
            "de" to "de-DE",
            "it" to "it-IT",
            "ar" to "ar-SA",
            "fr" to "fr-FR"
        )

        val normalized = partialTag.lowercase(Locale.ROOT)

        return supportedTags[normalized]
            ?: Locale.getDefault().toLanguageTag()
    }


}