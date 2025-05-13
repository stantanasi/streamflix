package com.tanasi.streamflix.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import com.tanasi.streamflix.BuildConfig
import com.tanasi.streamflix.R
import com.tanasi.streamflix.fragments.player.settings.PlayerSettingsView
import com.tanasi.streamflix.providers.Provider
import com.tanasi.streamflix.providers.Provider.Companion.providers

object UserPreferences {

    private lateinit var prefs: SharedPreferences

    fun setup(context: Context) {
        prefs = context.getSharedPreferences(
            "${BuildConfig.APPLICATION_ID}.preferences",
            Context.MODE_PRIVATE,
        )
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

    var streamingcommunityDomain: String?
        get() = Key.STREAMINGCOMMUNITY_DOMAIN.getString()
        set(value) = Key.STREAMINGCOMMUNITY_DOMAIN.setString(value)

    var streamingcommunityDnsOverHttps: Boolean
        get() = Key.STREAMINGCOMMUNITY_DNS_OVER_HTTPS.getBoolean()?: true
        set(value) = Key.STREAMINGCOMMUNITY_DNS_OVER_HTTPS.setBoolean(value)


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
        STREAMINGCOMMUNITY_DNS_OVER_HTTPS;

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
            prefs.contains(name) -> prefs.getString(name, "")
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