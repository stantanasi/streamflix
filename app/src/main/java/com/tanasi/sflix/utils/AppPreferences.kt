package com.tanasi.sflix.utils

import android.content.Context
import android.content.SharedPreferences
import com.tanasi.sflix.BuildConfig
import com.tanasi.sflix.providers.AllMoviesForYouProvider
import com.tanasi.sflix.providers.Provider
import com.tanasi.sflix.providers.SflixProvider

object AppPreferences {

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

    var currentProvider: Provider
        get() = Key.CURRENT_PROVIDER.getString()?.let { name ->
            providers.find { it.name == name }
        } ?: SflixProvider
        set(value) = Key.CURRENT_PROVIDER.setString(value.name)


    private enum class Key {
        CURRENT_PROVIDER;

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