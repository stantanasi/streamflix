package com.tanasi.sflix.presenters

import android.view.ViewGroup
import androidx.leanback.widget.Presenter
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.tanasi.sflix.databinding.ItemMovieBinding
import com.tanasi.sflix.models.Movie

class MoviePresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
        TODO("Not yet implemented")
    }

    override fun onBindViewHolder(viewHolder: ViewHolder?, item: Any?) {
        TODO("Not yet implemented")
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder?) {
        TODO("Not yet implemented")
    }

    class VhMovie(
        private val _binding: ViewBinding
    ) : ViewHolder(
        _binding.root
    ) {

        private val context = view.context
        private lateinit var movie: Movie

        fun bind(movie: Movie) {
            this.movie = movie

            when (_binding) {
                is ItemMovieBinding -> displayMovie(_binding)
            }
        }


        private fun displayMovie(binding: ItemMovieBinding) {
            Glide.with(context)
                .load(movie.poster)
                .centerCrop()
                .into(binding.ivMoviePoster)

            binding.tvMovieTitle.text = movie.title
        }
    }
}