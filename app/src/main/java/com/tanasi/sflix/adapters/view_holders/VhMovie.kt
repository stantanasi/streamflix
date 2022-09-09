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
import com.tanasi.sflix.fragments.home.HomeFragment
import com.tanasi.sflix.fragments.home.HomeFragmentDirections
import com.tanasi.sflix.fragments.movie.MovieFragment
import com.tanasi.sflix.fragments.movie.MovieFragmentDirections
import com.tanasi.sflix.fragments.movies.MoviesFragment
import com.tanasi.sflix.fragments.movies.MoviesFragmentDirections
import com.tanasi.sflix.fragments.people.PeopleFragment
import com.tanasi.sflix.fragments.people.PeopleFragmentDirections
import com.tanasi.sflix.fragments.search.SearchFragment
import com.tanasi.sflix.fragments.search.SearchFragmentDirections
import com.tanasi.sflix.fragments.tv_show.TvShowFragment
import com.tanasi.sflix.fragments.tv_show.TvShowFragmentDirections
import com.tanasi.sflix.models.Movie
import com.tanasi.sflix.models.TvShow
import com.tanasi.sflix.utils.format
import com.tanasi.sflix.utils.getCurrentFragment
import com.tanasi.sflix.utils.toActivity

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
            is ItemMovieBinding -> displayItem(_binding)
            is ItemMovieGridBinding -> displayGridItem(_binding)

            is ContentMovieBinding -> displayMovie(_binding)
            is ContentMovieCastsBinding -> displayCasts(_binding)
            is ContentMovieRecommendationsBinding -> displayRecommendations(_binding)
        }
    }


    private fun displayItem(binding: ItemMovieBinding) {
        binding.root.apply {
            setOnClickListener {
                when (context.toActivity()?.getCurrentFragment()) {
                    is HomeFragment -> findNavController().navigate(
                        HomeFragmentDirections.actionHomeToMovie(
                            id = movie.id
                        )
                    )
                    is MovieFragment -> findNavController().navigate(
                        MovieFragmentDirections.actionMovieToMovie(
                            id = movie.id
                        )
                    )
                    is PeopleFragment -> findNavController().navigate(
                        PeopleFragmentDirections.actionPeopleToMovie(
                            id = movie.id
                        )
                    )
                    is TvShowFragment -> findNavController().navigate(
                        TvShowFragmentDirections.actionTvShowToMovie(
                            id = movie.id
                        )
                    )
                }
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

    private fun displayGridItem(binding: ItemMovieGridBinding) {
        binding.root.apply {
            setOnClickListener {
                when (context.toActivity()?.getCurrentFragment()) {
                    is MoviesFragment -> findNavController().navigate(
                        MoviesFragmentDirections.actionMoviesToMovie(
                            id = movie.id
                        )
                    )
                    is SearchFragment -> findNavController().navigate(
                        SearchFragmentDirections.actionSearchToMovie(
                            id = movie.id
                        )
                    )
                }

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


    private fun displayMovie(binding: ContentMovieBinding) {
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

    private fun displayCasts(binding: ContentMovieCastsBinding) {
        binding.hgvMovieCasts.apply {
            setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
            adapter = SflixAdapter(movie.casts)
            setItemSpacing(80)
        }
    }

    private fun displayRecommendations(binding: ContentMovieRecommendationsBinding) {
        binding.hgvMovieRecommendations.apply {
            setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
            adapter = SflixAdapter(movie.recommendations.onEach {
                when (it) {
                    is Movie -> it.itemType = SflixAdapter.Type.MOVIE_ITEM
                    is TvShow -> it.itemType = SflixAdapter.Type.TV_SHOW_ITEM
                }
            })
            setItemSpacing(20)
        }
    }
}