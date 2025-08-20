package com.tanasi.streamflix.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log // <-- Import Log
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import com.tanasi.streamflix.BuildConfig
import com.tanasi.streamflix.R
import com.tanasi.streamflix.fragments.player.settings.PlayerSettingsView
import com.tanasi.streamflix.providers.Provider
import com.tanasi.streamflix.providers.Provider.Companion.providers
import androidx.core.content.edit

object UserPreferences {

    private const val TAG = "UserPrefsDebug" // <-- TAG per i Log

    private lateinit var prefs: SharedPreferences

    // Default DoH Provider URL (Cloudflare)
    private const val DEFAULT_DOH_PROVIDER_URL = "https://cloudflare-dns.com/dns-query"
    const val DOH_DISABLED_VALUE = "" // Value to represent DoH being disabled
    private const val DEFAULT_STREAMINGCOMMUNITY_DOMAIN = "streamingcommunityz.life"

    fun setup(context: Context) {
        Log.d(TAG, "setup() called with context: $context")
        val prefsName = "${BuildConfig.APPLICATION_ID}.preferences"
        Log.d(TAG, "SharedPreferences name: $prefsName")
        prefs = context.getSharedPreferences(
            prefsName,
            Context.MODE_PRIVATE,
        )
        if (::prefs.isInitialized) {
            Log.d(TAG, "prefs initialized successfully in setup. Hash: ${prefs.hashCode()}")
        } else {
            Log.e(TAG, "prefs FAILED to initialize in setup.")
        }
    }


    var currentProvider: Provider?
        get() = providers.find { it.name == Key.CURRENT_PROVIDER.getString() }
        set(value) = Key.CURRENT_PROVIDER.setString(value?.name)

    var currentLanguage: String?
        get() = Key.CURRENT_LANGUAGE.getString()
        set(value) = Key.CURRENT_LANGUAGE.setString(value)

    var captionTextSize: Float
        get() = Key.CAPTION_TEXT_SIZE.getFloat()
            ?: PlayerSettingsView.Settings.Subtitle.Style.TextSize.DEFAULT.value
        set(value) {
            Key.CAPTION_TEXT_SIZE.setFloat(value)
        }

    var autoplay: Boolean
        get() = Key.AUTOPLAY.getBoolean() ?: true
        set(value) {
            Key.AUTOPLAY.setBoolean(value)
        }
    enum class PlayerResize(
        val stringRes: Int,
        val resizeMode: Int,
    ) {
        Fit(R.string.player_aspect_ratio_fit, AspectRatioFrameLayout.RESIZE_MODE_FIT),
        Fill(R.string.player_aspect_ratio_fill, AspectRatioFrameLayout.RESIZE_MODE_FILL),
        Zoom(R.string.player_aspect_ratio_zoom, AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    }

    var playerResize: PlayerResize
        get() = PlayerResize.entries.find { it.resizeMode == Key.PLAYER_RESIZE.getInt() }
            ?: PlayerResize.Fit
        set(value) {
            Key.PLAYER_RESIZE.setInt(value.resizeMode)
        }

    var captionStyle: CaptionStyleCompat
        get() = CaptionStyleCompat(
            Key.CAPTION_STYLE_FONT_COLOR.getInt()
                ?: PlayerSettingsView.Settings.Subtitle.Style.DEFAULT.foregroundColor,
            Key.CAPTION_STYLE_BACKGROUND_COLOR.getInt()
                ?: PlayerSettingsView.Settings.Subtitle.Style.DEFAULT.backgroundColor,
            Key.CAPTION_STYLE_WINDOW_COLOR.getInt()
                ?: PlayerSettingsView.Settings.Subtitle.Style.DEFAULT.windowColor,
            Key.CAPTION_STYLE_EDGE_TYPE.getInt()
                ?: PlayerSettingsView.Settings.Subtitle.Style.DEFAULT.edgeType,
            Key.CAPTION_STYLE_EDGE_COLOR.getInt()
                ?: PlayerSettingsView.Settings.Subtitle.Style.DEFAULT.edgeColor,
            PlayerSettingsView.Settings.Subtitle.Style.DEFAULT.typeface
        )
        set(value) {
            Key.CAPTION_STYLE_FONT_COLOR.setInt(value.foregroundColor)
            Key.CAPTION_STYLE_BACKGROUND_COLOR.setInt(value.backgroundColor)
            Key.CAPTION_STYLE_WINDOW_COLOR.setInt(value.windowColor)
            Key.CAPTION_STYLE_EDGE_TYPE.setInt(value.edgeType)
            Key.CAPTION_STYLE_EDGE_COLOR.setInt(value.edgeColor)
        }

    var qualityHeight: Int?
        get() = Key.QUALITY_HEIGHT.getInt()
        set(value) {
            Key.QUALITY_HEIGHT.setInt(value)
        }

    var subtitleName: String?
        get() = Key.SUBTITLE_NAME.getString()
        set(value) {
            Key.SUBTITLE_NAME.setString(value)
        }
    var streamingcommunityDomain: String
        get() {
            Log.d(TAG, "streamingcommunityDomain GET called")
            if (!::prefs.isInitialized) {
                Log.e(TAG, "streamingcommunityDomain GET: prefs IS NOT INITIALIZED!")
                return "PREFS_NOT_INIT_ERROR" // Restituisce un valore di errore evidente
            }
            Log.d(TAG, "streamingcommunityDomain GET: prefs hash: ${prefs.hashCode()}")
            val storedValue = prefs.getString(Key.STREAMINGCOMMUNITY_DOMAIN.name, null)
            Log.d(TAG, "streamingcommunityDomain GET: storedValue from prefs: '$storedValue'")
            val returnValue = if (storedValue.isNullOrEmpty()) {
                Log.d(TAG, "streamingcommunityDomain GET: storedValue is null or empty, returning DEFAULT: '$DEFAULT_STREAMINGCOMMUNITY_DOMAIN'")
                DEFAULT_STREAMINGCOMMUNITY_DOMAIN
            } else {
                Log.d(TAG, "streamingcommunityDomain GET: storedValue is NOT null or empty, returning storedValue: '$storedValue'")
                storedValue
            }
            Log.d(TAG, "streamingcommunityDomain GET: final returnValue: '$returnValue'")
            return returnValue
        }
        set(value) {
            Log.d(TAG, "streamingcommunityDomain SET called with value: '$value'")
            if (!::prefs.isInitialized) {
                Log.e(TAG, "streamingcommunityDomain SET: prefs IS NOT INITIALIZED!")
                return // Non fare nulla se prefs non è inizializzato
            }
            Log.d(TAG, "streamingcommunityDomain SET: prefs hash: ${prefs.hashCode()}")
            with(prefs.edit()) {
                if (value.isNullOrEmpty()) {
                    Log.d(TAG, "streamingcommunityDomain SET: value is null or empty, REMOVING key: '${Key.STREAMINGCOMMUNITY_DOMAIN.name}'")
                    remove(Key.STREAMINGCOMMUNITY_DOMAIN.name)
                } else {
                    Log.d(TAG, "streamingcommunityDomain SET: value is NOT null or empty, PUTTING STRING key: '${Key.STREAMINGCOMMUNITY_DOMAIN.name}', value: '$value'")
                    putString(Key.STREAMINGCOMMUNITY_DOMAIN.name, value)
                }
                apply()
                Log.d(TAG, "streamingcommunityDomain SET: prefs.edit().apply() called")
            }
        }

    var dohProviderUrl: String
        get() = Key.DOH_PROVIDER_URL.getString() ?: DEFAULT_DOH_PROVIDER_URL
        set(value) = Key.DOH_PROVIDER_URL.setString(value)


    private enum class Key {
        APP_LAYOUT,
        CURRENT_LANGUAGE,
        CURRENT_PROVIDER,
        PLAYER_RESIZE,
        CAPTION_TEXT_SIZE,
        CAPTION_STYLE_FONT_COLOR,
        CAPTION_STYLE_BACKGROUND_COLOR,
        CAPTION_STYLE_WINDOW_COLOR,
        CAPTION_STYLE_EDGE_TYPE,
        CAPTION_STYLE_EDGE_COLOR,
        QUALITY_HEIGHT,
        SUBTITLE_NAME,
        STREAMINGCOMMUNITY_DOMAIN,
        DOH_PROVIDER_URL, // Removed STREAMINGCOMMUNITY_DNS_OVER_HTTPS, added DOH_PROVIDER_URL
        AUTOPLAY;

        fun getBoolean(): Boolean? = when {
            prefs.contains(name) -> prefs.getBoolean(name, false)
            else -> null
        }

        fun getFloat(): Float? = when {
            prefs.contains(name) -> prefs.getFloat(name, 0F)
            else -> null
        }

        fun getInt(): Int? = when {
            prefs.contains(name) -> prefs.getInt(name, 0)
            else -> null
        }

        fun getLong(): Long? = when {
            prefs.contains(name) -> prefs.getLong(name, 0)
            else -> null
        }

        fun getString(): String? = when {
            prefs.contains(name) -> prefs.getString(name, null)
            else -> null
        }

        fun setBoolean(value: Boolean?) = value?.let {
            with(prefs.edit()) {
                putBoolean(name, value)
                apply()
            }
        } ?: remove()

        fun setFloat(value: Float?) = value?.let {
            with(prefs.edit()) {
                putFloat(name, value)
                apply()
            }
        } ?: remove()

        fun setInt(value: Int?) = value?.let {
            with(prefs.edit()) {
                putInt(name, value)
                apply()
            }
        } ?: remove()

        fun setLong(value: Long?) = value?.let {
            with(prefs.edit()) {
                putLong(name, value)
                apply()
            }
        } ?: remove()

        fun setString(value: String?) = value?.let {
            with(prefs.edit()) {
                putString(name, value)
                apply()
            }
        } ?: remove()

        fun remove() = with(prefs.edit()) {
            remove(name)
            apply()
        }
    }
}