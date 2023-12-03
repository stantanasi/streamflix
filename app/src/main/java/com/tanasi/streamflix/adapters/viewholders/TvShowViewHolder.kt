package com.tanasi.streamflix.adapters.viewholders

import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.tanasi.streamflix.R
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.databinding.ContentTvShowBinding
import com.tanasi.streamflix.databinding.ContentTvShowCastsBinding
import com.tanasi.streamflix.databinding.ContentTvShowRecommendationsBinding
import com.tanasi.streamflix.databinding.ContentTvShowSeasonsBinding
import com.tanasi.streamflix.databinding.ItemTvShowBinding
import com.tanasi.streamflix.databinding.ItemTvShowGridBinding
import com.tanasi.streamflix.fragments.genre.GenreFragment
import com.tanasi.streamflix.fragments.genre.GenreFragmentDirections
import com.tanasi.streamflix.fragments.home.HomeFragment
import com.tanasi.streamflix.fragments.home.HomeFragmentDirections
import com.tanasi.streamflix.fragments.movie.MovieFragment
import com.tanasi.streamflix.fragments.movie.MovieFragmentDirections
import com.tanasi.streamflix.fragments.people.PeopleFragment
import com.tanasi.streamflix.fragments.people.PeopleFragmentDirections
import com.tanasi.streamflix.fragments.search.SearchFragment
import com.tanasi.streamflix.fragments.search.SearchFragmentDirections
import com.tanasi.streamflix.fragments.tv_show.TvShowFragment
import com.tanasi.streamflix.fragments.tv_show.TvShowFragmentDirections
import com.tanasi.streamflix.fragments.tv_shows.TvShowsFragment
import com.tanasi.streamflix.fragments.tv_shows.TvShowsFragmentDirections
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.utils.format
import com.tanasi.streamflix.utils.getCurrentFragment
import com.tanasi.streamflix.utils.toActivity

class TvShowViewHolder(
    private val _binding: ViewBinding
) : RecyclerView.ViewHolder(
    _binding.root
) {

    private val context = itemView.context
    private lateinit var tvShow: TvShow

    val childRecyclerView: RecyclerView?
        get() = when (_binding) {
            is ContentTvShowSeasonsBinding -> _binding.hgvTvShowSeasons
            is ContentTvShowCastsBinding -> _binding.hgvTvShowCasts
            is ContentTvShowRecommendationsBinding -> _binding.hgvTvShowRecommendations
            else -> null
        }

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

                if (hasFocus) {
                    when (val fragment = context.toActivity()?.getCurrentFragment()) {
                        is HomeFragment -> fragment.updateBackground(tvShow.banner)
                    }
                }
            }
        }

        Glide.with(context)
            .load(tvShow.poster)
            .centerCrop()
            .into(binding.ivTvShowPoster)

        binding.tvTvShowQuality.apply {
            text = tvShow.quality ?: ""
            visibility = when {
                tvShow.quality.isNullOrEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.tvTvShowLastEpisode.text = tvShow.seasons.lastOrNull()?.let { season ->
            season.episodes.lastOrNull()?.let { episode ->
                if (season.number != 0) {
                    context.getString(
                        R.string.tv_show_item_season_number_episode_number,
                        season.number,
                        episode.number
                    )
                } else {
                    context.getString(
                        R.string.tv_show_item_episode_number,
                        episode.number
                    )
                }
            }
        } ?: context.getString(R.string.tv_show_item_type)

        binding.tvTvShowTitle.text = tvShow.title
    }

    private fun displayGridItem(binding: ItemTvShowGridBinding) {
        binding.root.apply {
            setOnClickListener {
                when (context.toActivity()?.getCurrentFragment()) {
                    is GenreFragment -> findNavController().navigate(
                        GenreFragmentDirections.actionGenreToTvShow(
                            id = tvShow.id
                        )
                    )
                    is PeopleFragment -> findNavController().navigate(
                        PeopleFragmentDirections.actionPeopleToTvShow(
                            id = tvShow.id
                        )
                    )
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

        binding.tvTvShowQuality.apply {
            text = tvShow.quality ?: ""
            visibility = when {
                tvShow.quality.isNullOrEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.tvTvShowLastEpisode.text = tvShow.seasons.lastOrNull()?.let { season ->
            season.episodes.lastOrNull()?.let { episode ->
                if (season.number != 0) {
                    context.getString(
                        R.string.tv_show_item_season_number_episode_number,
                        season.number,
                        episode.number
                    )
                } else {
                    context.getString(
                        R.string.tv_show_item_episode_number,
                        episode.number
                    )
                }
            }
        } ?: context.getString(R.string.tv_show_item_type)

        binding.tvTvShowTitle.text = tvShow.title
    }


    private fun displayTvShow(binding: ContentTvShowBinding) {
        binding.ivTvShowPoster.run {
            Glide.with(context)
                .load(tvShow.poster)
                .into(this)
            visibility = when {
                tvShow.poster.isNullOrEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.tvTvShowTitle.text = tvShow.title

        binding.tvTvShowRating.text = tvShow.rating?.let { String.format("%.1f", it) } ?: "N/A"

        binding.tvTvShowQuality.apply {
            text = tvShow.quality
            visibility = when {
                text.isNullOrEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.tvTvShowReleased.apply {
            text = tvShow.released?.format("yyyy")
            visibility = when {
                text.isNullOrEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.tvTvShowRuntime.apply {
            text = tvShow.runtime?.let {
                val hours = it / 60
                val minutes = it % 60
                when {
                    hours > 0 -> context.getString(
                        R.string.tv_show_runtime_hours_minutes,
                        hours,
                        minutes
                    )
                    else -> context.getString(R.string.tv_show_runtime_minutes, minutes)
                }
            }
            visibility = when {
                text.isNullOrEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.tvTvShowGenres.apply {
            text = tvShow.genres.joinToString(", ") { it.name }
            visibility = when {
                tvShow.genres.isEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

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
            adapter = AppAdapter().apply {
                items.addAll(tvShow.seasons)
            }
            setItemSpacing(80)
        }
    }

    private fun displayCasts(binding: ContentTvShowCastsBinding) {
        binding.hgvTvShowCasts.apply {
            setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
            adapter = AppAdapter().apply {
                items.addAll(tvShow.cast)
            }
            setItemSpacing(80)
        }
    }

    private fun displayRecommendations(binding: ContentTvShowRecommendationsBinding) {
        binding.hgvTvShowRecommendations.apply {
            setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
            adapter = AppAdapter().apply {
                items.addAll(tvShow.recommendations.onEach {
                    when (it) {
                        is Movie -> it.itemType = AppAdapter.Type.MOVIE_ITEM
                        is TvShow -> it.itemType = AppAdapter.Type.TV_SHOW_ITEM
                    }
                })
            }
            setItemSpacing(20)
        }
    }
}