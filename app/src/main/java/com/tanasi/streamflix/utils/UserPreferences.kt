package com.tanasi.streamflix.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.android.exoplayer2.ui.CaptionStyleCompat
import com.tanasi.streamflix.BuildConfig
import com.tanasi.streamflix.providers.AllMoviesForYouProvider
import com.tanasi.streamflix.providers.Provider
import com.tanasi.streamflix.providers.SflixProvider
import com.tanasi.streamflix.ui.PlayerSettingsView

object UserPreferences {

    private lateinit var prefs: SharedPreferences

    fun setup(context: Context) {
        prefs = context.getSharedPreferences(
            "${BuildConfig.APPLICATION_ID}.preferences",
            Context.MODE_PRIVATE,
        )
    }


    val providers = listOf(
        SflixProvider,
        AllMoviesForYouProvider,
    )

    var currentProvider: Provider?
        get() = providers.find { it.name == Key.CURRENT_PROVIDER.getString() }
        set(value) = Key.CURRENT_PROVIDER.setString(value?.name)

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


    private enum class Key {
        CURRENT_PROVIDER,
        CAPTION_STYLE_FONT_COLOR,
        CAPTION_STYLE_BACKGROUND_COLOR,
        CAPTION_STYLE_WINDOW_COLOR,
        CAPTION_STYLE_EDGE_TYPE,
        CAPTION_STYLE_EDGE_COLOR;

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