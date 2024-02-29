package com.tanasi.streamflix.ui

import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.databinding.DialogShowOptionsBinding
import com.tanasi.streamflix.models.Episode
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.models.TvShow

class ShowOptionsDialog(context: Context) : Dialog(context) {

    private val binding = DialogShowOptionsBinding.inflate(LayoutInflater.from(context))

    var show: AppAdapter.Item? = null
        set(value) {
            when (value) {
                is Episode -> displayEpisode(value)
                is Movie -> displayMovie(value)
                is TvShow -> displayTvShow(value)
            }
            field = value
        }

    init {
        setContentView(binding.root)

        binding.btnOptionCancel.setOnClickListener {
            hide()
        }


        window?.attributes = window?.attributes?.also { param ->
            param.gravity = Gravity.END
        }
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.35).toInt(),
            context.resources.displayMetrics.heightPixels
        )
    }


    private fun displayEpisode(episode: Episode) {
    }

    private fun displayMovie(movie: Movie) {
    }

    private fun displayTvShow(tvShow: TvShow) {
    }
}