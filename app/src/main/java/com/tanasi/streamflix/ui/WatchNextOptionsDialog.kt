package com.tanasi.streamflix.ui

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import androidx.tvprovider.media.tv.TvContractCompat
import androidx.tvprovider.media.tv.WatchNextProgram
import com.bumptech.glide.Glide
import com.tanasi.streamflix.R
import com.tanasi.streamflix.databinding.DialogWatchNextOptionsBinding
import com.tanasi.streamflix.utils.format
import com.tanasi.streamflix.utils.toCalendar


@SuppressLint("RestrictedApi")
class WatchNextOptionsDialog(context: Context) : Dialog(context) {

    private val binding = DialogWatchNextOptionsBinding.inflate(LayoutInflater.from(context))

    var program: WatchNextProgram? = null
        set(value) {
            when (value?.type) {
                TvContractCompat.PreviewPrograms.TYPE_MOVIE -> {
                    binding.tvProgramTitle.text = value.title
                    binding.tvProgramSubtitle.text = value.releaseDate?.toCalendar()?.format("yyyy")
                    Glide.with(context)
                        .load(value.posterArtUri?.toString())
                        .centerCrop()
                        .into(binding.ivProgramPoster)
                }

                TvContractCompat.PreviewPrograms.TYPE_TV_EPISODE -> {
                    binding.tvProgramTitle.text = value.title
                    binding.tvProgramSubtitle.text =
                        value.seasonNumber?.toIntOrNull()?.let { seasonNumber ->
                            context.getString(
                                R.string.episode_item_info,
                                seasonNumber,
                                value.episodeNumber?.toIntOrNull() ?: 0,
                                value.episodeTitle ?: "",
                            )
                        } ?: context.getString(
                            R.string.episode_item_info_episode_only,
                            value.episodeNumber?.toIntOrNull() ?: 0,
                            value.episodeTitle ?: "",
                        )
                    Glide.with(context)
                        .load(value.posterArtUri?.toString())
                        .centerCrop()
                        .into(binding.ivProgramPoster)
                }

                else -> {}
            }
            field = value
        }

    init {
        setContentView(binding.root)

        binding.btnProgramClear.requestFocus()

        binding.btnProgramCancel.setOnClickListener {
            hide()
        }


        window?.attributes = window?.attributes?.also { param ->
            param.gravity = Gravity.END
        }
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.35).toInt(),
            (context.resources.displayMetrics.heightPixels * 0.90).toInt()
        )
    }


    fun setOnProgramClearListener(listener: (view: View) -> Unit) {
        binding.btnProgramClear.setOnClickListener(listener)
    }
}