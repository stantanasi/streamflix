package com.tanasi.sflix.adapters.view_holders

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.tanasi.sflix.R
import com.tanasi.sflix.adapters.SflixAdapter
import com.tanasi.sflix.databinding.*
import com.tanasi.sflix.fragments.home.HomeFragmentDirections
import com.tanasi.sflix.fragments.movie.MovieFragmentDirections
import com.tanasi.sflix.fragments.movies.MoviesFragmentDirections
import com.tanasi.sflix.fragments.search.SearchFragmentDirections
import com.tanasi.sflix.models.Movie
import com.tanasi.sflix.utils.format

class VhMovie(
    private val _binding: ViewBinding
) : RecyclerView.ViewHolder(
    _binding.root
) {

    private val context = itemView.context
    private lateinit var movie: Movie

    fun bind(movie: Movie) {
        this.movie = movie

        when (_binding) {
            is ItemMovieHomeBinding -> displayHome(_binding)
            is ItemMovieMoviesBinding -> displayMovies(_binding)
            is ItemMovieSearchBinding -> displaySearch(_binding)

            is ItemMovieHeaderBinding -> displayHeader(_binding)
            is ItemMovieCastsBinding -> displayCasts(_binding)
        }
    }


    private fun displayHome(binding: ItemMovieHomeBinding) {
        binding.root.apply {
            setOnClickListener {
                findNavController().navigate(
                    HomeFragmentDirections.actionHomeToMovie(
                        id = movie.id
                    )
                )
            }
            setOnFocusChangeListener { _, hasFocus ->
                val animation = when {
                    hasFocus -> AnimationUtils.loadAnimation(context, R.anim.zoom_in)
                    else -> AnimationUtils.loadAnimation(context, R.anim.zoom_out)
                }
                binding.root.startAnimation(animation)
                animation.fillAfter = true
            }
        }

        Glide.with(context)
            .load(movie.poster)
            .centerCrop()
            .into(binding.ivMoviePoster)

        binding.tvMovieQuality.text = movie.quality?.name ?: "N/A"

        binding.tvMovieReleasedYear.text = movie.released?.format("yyyy") ?: ""

        binding.tvMovieTitle.text = movie.title
    }

    private fun displayMovies(binding: ItemMovieMoviesBinding) {
        binding.root.apply {
            setOnClickListener {
                findNavController().navigate(
                    MoviesFragmentDirections.actionMoviesToMovie(
                        id = movie.id
                    )
                )
            }
            setOnFocusChangeListener { _, hasFocus ->
                val animation = when {
                    hasFocus -> AnimationUtils.loadAnimation(context, R.anim.zoom_in)
                    else -> AnimationUtils.loadAnimation(context, R.anim.zoom_out)
                }
                binding.root.startAnimation(animation)
                animation.fillAfter = true
            }
        }

        Glide.with(context)
            .load(movie.poster)
            .centerCrop()
            .into(binding.ivMoviePoster)

        binding.tvMovieQuality.text = movie.quality?.name ?: "N/A"

        binding.tvMovieReleasedYear.text = movie.released?.format("yyyy") ?: ""

        binding.tvMovieTitle.text = movie.title
    }

    private fun displaySearch(binding: ItemMovieSearchBinding) {
        binding.root.apply {
            setOnClickListener {
                findNavController().navigate(
                    SearchFragmentDirections.actionSearchToMovie(
                        id = movie.id
                    )
                )
            }
            setOnFocusChangeListener { _, hasFocus ->
                val animation = when {
                    hasFocus -> AnimationUtils.loadAnimation(context, R.anim.zoom_in)
                    else -> AnimationUtils.loadAnimation(context, R.anim.zoom_out)
                }
                binding.root.startAnimation(animation)
                animation.fillAfter = true
            }
        }

        Glide.with(context)
            .load(movie.poster)
            .centerCrop()
            .into(binding.ivMoviePoster)

        binding.tvMovieQuality.text = movie.quality?.name ?: "N/A"

        binding.tvMovieReleasedYear.text = movie.released?.format("yyyy") ?: ""

        binding.tvMovieTitle.text = movie.title
    }


    private fun displayHeader(binding: ItemMovieHeaderBinding) {
        Glide.with(context)
            .load(movie.poster)
            .into(binding.ivMoviePoster)

        binding.tvMovieTitle.text = movie.title

        binding.tvMovieRating.text = movie.rating?.let { String.format("%.1f", it) } ?: "N/A"

        binding.tvMovieQuality.text = movie.quality?.name ?: "N/A"

        binding.tvMovieReleased.text = movie.released?.format("yyyy")

        binding.tvMovieRuntime.text = movie.runtime?.let {
            val hours = it / 60
            val minutes = it % 60
            when {
                hours > 0 -> "$hours h $minutes min"
                else -> "$minutes min"
            }
        } ?: "0 min"

        binding.tvMovieOverview.text = movie.overview

        binding.btnMovieWatchNow.apply {
            setOnClickListener {
                findNavController().navigate(
                    MovieFragmentDirections.actionMovieToPlayer(
                        linkId = movie.servers.firstOrNull()?.id ?: "",
                    )
                )
            }
        }

        binding.btnMovieTrailer.setOnClickListener {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.youtube.com/watch?v=${movie.youtubeTrailerId}")
                )
            )
        }
    }

    private fun displayCasts(binding: ItemMovieCastsBinding) {
        binding.hgvMoviePeoples.apply {
            setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
            adapter = SflixAdapter(movie.casts)
            setItemSpacing(80)
        }
    }
}