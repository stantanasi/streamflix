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
import com.tanasi.sflix.fragments.people.PeopleFragment
import com.tanasi.sflix.fragments.people.PeopleFragmentDirections
import com.tanasi.sflix.fragments.search.SearchFragment
import com.tanasi.sflix.fragments.search.SearchFragmentDirections
import com.tanasi.sflix.fragments.tv_show.TvShowFragment
import com.tanasi.sflix.fragments.tv_show.TvShowFragmentDirections
import com.tanasi.sflix.fragments.tv_shows.TvShowsFragment
import com.tanasi.sflix.fragments.tv_shows.TvShowsFragmentDirections
import com.tanasi.sflix.models.Movie
import com.tanasi.sflix.models.TvShow
import com.tanasi.sflix.utils.format
import com.tanasi.sflix.utils.getCurrentFragment
import com.tanasi.sflix.utils.toActivity

class VhTvShow(
    private val _binding: ViewBinding
) : RecyclerView.ViewHolder(
    _binding.root
) {

    private val context = itemView.context
    private lateinit var tvShow: TvShow

    fun bind(tvShow: TvShow) {
        this.tvShow = tvShow

        when (_binding) {
            is ItemTvShowBinding -> displayItem(_binding)
            is ItemTvShowGridBinding -> displayGridItem(_binding)

            is ContentTvShowBinding -> displayTvShow(_binding)
            is ContentTvShowSeasonsBinding -> displaySeasons(_binding)
            is ContentTvShowCastsBinding -> displayCasts(_binding)
            is ContentTvShowRecommendationsBinding -> displayRecommendations(_binding)
        }
    }


    private fun displayItem(binding: ItemTvShowBinding) {
        binding.root.apply {
            setOnClickListener {
                when (context.toActivity()?.getCurrentFragment()) {
                    is HomeFragment -> findNavController().navigate(
                        HomeFragmentDirections.actionHomeToTvShow(
                            id = tvShow.id
                        )
                    )
                    is MovieFragment -> findNavController().navigate(
                        MovieFragmentDirections.actionMovieToTvShow(
                            id = tvShow.id
                        )
                    )
                    is PeopleFragment -> findNavController().navigate(
                        PeopleFragmentDirections.actionPeopleToTvShow(
                            id = tvShow.id
                        )
                    )
                    is TvShowFragment -> findNavController().navigate(
                        TvShowFragmentDirections.actionTvShowToTvShow(
                            id = tvShow.id
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
            .load(tvShow.poster)
            .centerCrop()
            .into(binding.ivTvShowPoster)

        binding.tvTvShowQuality.text = tvShow.quality?.name ?: "N/A"

        binding.tvTvShowLastEpisode.text =
            "S${tvShow.seasons.lastOrNull()?.number ?: ""} E${tvShow.seasons.lastOrNull()?.episodes?.lastOrNull()?.number ?: ""}"

        binding.tvTvShowTitle.text = tvShow.title
    }

    private fun displayGridItem(binding: ItemTvShowGridBinding) {
        binding.root.apply {
            setOnClickListener {
                when (context.toActivity()?.getCurrentFragment()) {
                    is SearchFragment -> findNavController().navigate(
                        SearchFragmentDirections.actionSearchToTvShow(
                            id = tvShow.id
                        )
                    )
                    is TvShowsFragment -> findNavController().navigate(
                        TvShowsFragmentDirections.actionTvShowsToTvShow(
                            id = tvShow.id
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
            .load(tvShow.poster)
            .centerCrop()
            .into(binding.ivTvShowPoster)

        binding.tvTvShowQuality.text = tvShow.quality?.name ?: "N/A"

        binding.tvTvShowLastEpisode.text =
            "S${tvShow.seasons.lastOrNull()?.number ?: ""} E${tvShow.seasons.lastOrNull()?.episodes?.lastOrNull()?.number ?: ""}"

        binding.tvTvShowTitle.text = tvShow.title
    }


    private fun displayTvShow(binding: ContentTvShowBinding) {
        Glide.with(context)
            .load(tvShow.poster)
            .into(binding.ivTvShowPoster)

        binding.tvTvShowTitle.text = tvShow.title

        binding.tvTvShowRating.text = tvShow.rating?.let { String.format("%.1f", it) } ?: "N/A"

        binding.tvTvShowQuality.text = tvShow.quality?.name ?: "N/A"

        binding.tvTvShowReleased.text = tvShow.released?.format("yyyy")

        binding.tvTvShowRuntime.text = tvShow.runtime?.let {
            val hours = it / 60
            val minutes = it % 60
            when {
                hours > 0 -> "$hours h $minutes min"
                else -> "$minutes min"
            }
        } ?: "0 min"

        binding.tvTvShowOverview.text = tvShow.overview

        binding.btnTvShowTrailer.setOnClickListener {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.youtube.com/watch?v=${tvShow.youtubeTrailerId}")
                )
            )
        }
    }

    private fun displaySeasons(binding: ContentTvShowSeasonsBinding) {
        binding.hgvTvShowSeasons.apply {
            setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
            adapter = SflixAdapter(tvShow.seasons.reversed())
            setItemSpacing(80)
        }
    }

    private fun displayCasts(binding: ContentTvShowCastsBinding) {
        binding.hgvTvShowCasts.apply {
            setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
            adapter = SflixAdapter(tvShow.casts)
            setItemSpacing(80)
        }
    }

    private fun displayRecommendations(binding: ContentTvShowRecommendationsBinding) {
        binding.hgvTvShowRecommendations.apply {
            setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
            adapter = SflixAdapter(tvShow.recommendations.onEach {
                when (it) {
                    is Movie -> it.itemType = SflixAdapter.Type.MOVIE_ITEM
                    is TvShow -> it.itemType = SflixAdapter.Type.TV_SHOW_ITEM
                }
            })
            setItemSpacing(20)
        }
    }
}