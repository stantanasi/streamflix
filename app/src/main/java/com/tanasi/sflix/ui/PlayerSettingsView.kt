package com.tanasi.sflix.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.ExoPlayer
import com.tanasi.sflix.R
import com.tanasi.sflix.databinding.ItemSettingBinding
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


    private class SettingsAdapter(
        private val settings: List<Setting>,
    ) : RecyclerView.Adapter<SettingViewHolder>() {

        lateinit var playerSettingsView: PlayerSettingsView

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingViewHolder {
            return SettingViewHolder(
                ItemSettingBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: SettingViewHolder, position: Int) {
            val setting = settings[position]

            holder.binding.root.setOnClickListener {
                playerSettingsView.displaySetting(setting)
            }

            holder.binding.ivSettingIcon.apply {
                when (setting) {
                    Setting.Quality -> setImageDrawable(
                        ContextCompat.getDrawable(context, R.drawable.ic_settings_quality)
                    )
                    Setting.Subtitle -> setImageDrawable(
                        ContextCompat.getDrawable(context, R.drawable.ic_settings_subtitle_off)
                    )
                    Setting.Speed -> setImageDrawable(
                        ContextCompat.getDrawable(context, R.drawable.ic_settings_playback_speed)
                    )
                    else -> {}
                }
                visibility = View.VISIBLE
            }

            holder.binding.tvSettingMainText.text = when (setting) {
                Setting.Quality -> "Quality"
                Setting.Subtitle -> "Captions"
                Setting.Speed -> "Speed"
                else -> ""
            }

            holder.binding.tvSettingSubText.apply {
                text = ""
                visibility = when (text) {
                    "" -> View.GONE
                    else -> View.VISIBLE
                }
            }

            holder.binding.ivSettingCheck.visibility = View.GONE
        }

        override fun getItemCount() = settings.size
    }


    private class SettingViewHolder(
        val binding: ItemSettingBinding,
    ) : RecyclerView.ViewHolder(binding.root)
}