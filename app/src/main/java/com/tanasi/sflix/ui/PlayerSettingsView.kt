package com.tanasi.sflix.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.exoplayer2.ExoPlayer
import com.tanasi.sflix.databinding.ViewPlayerSettingsBinding

class PlayerSettingsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    val binding = ViewPlayerSettingsBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

    var player: ExoPlayer? = null


    fun show() {
        this.visibility = View.VISIBLE

        displaySetting(Setting.Main)
    }

    private fun displaySetting(setting: Setting) {
        binding.tvSettingsHeader.text = when (setting) {
            Setting.Main -> "Settings"
            Setting.Quality -> "Quality for current video"
            Setting.Subtitle -> "Subtitles/closed captions"
            Setting.Speed -> "Video speed"
        }

        binding.rvSettings.adapter = when (setting) {
            Setting.Main -> TODO()
            Setting.Quality -> TODO()
            Setting.Subtitle -> TODO()
            Setting.Speed -> TODO()
        }
        binding.rvSettings.requestFocus()
    }

    fun hide() {
        this.visibility = View.GONE
    }


    private sealed class Setting {

        object Main : Setting()

        object Quality : Setting()

        object Subtitle : Setting()

        object Speed : Setting()
    }
}