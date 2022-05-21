package com.tanasi.sflix.presenters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.leanback.widget.Presenter
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.tanasi.sflix.databinding.ItemMovieBinding
import com.tanasi.sflix.models.Movie

class MoviePresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        return VhMovie(
            ItemMovieBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        when (viewHolder) {
            is VhMovie -> viewHolder.bind(item as Movie)
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
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