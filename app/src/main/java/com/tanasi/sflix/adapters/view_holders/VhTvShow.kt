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
import com.tanasi.sflix.fragments.search.SearchFragmentDirections
import com.tanasi.sflix.fragments.tv_show.TvShowFragmentDirections
import com.tanasi.sflix.fragments.tv_shows.TvShowsFragmentDirections
import com.tanasi.sflix.models.TvShow
import com.tanasi.sflix.utils.format

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
            is ItemTvShowHomeBinding -> displayHome(_binding)
            is ItemTvShowTvShowsBinding -> displayTvShows(_binding)
            is ItemTvShowSearchBinding -> displaySearch(_binding)

            is ItemTvShowHeaderBinding -> displayHeader(_binding)
            is ItemTvShowCastsBinding -> displayCasts(_binding)
        }
    }


    private fun displayHome(binding: ItemTvShowHomeBinding) {
        binding.root.apply {
            setOnClickListener {
                findNavController().navigate(
                    HomeFragmentDirections.actionHomeToTvShow(
                        id = tvShow.id
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
            .load(tvShow.poster)
            .centerCrop()
            .into(binding.ivTvShowPoster)

        binding.tvTvShowQuality.text = tvShow.quality?.name ?: "N/A"

        binding.tvTvShowLastEpisode.text =
            "S${tvShow.seasons.lastOrNull()?.number ?: ""} E${tvShow.seasons.lastOrNull()?.episodes?.lastOrNull()?.number ?: ""}"

        binding.tvTvShowTitle.text = tvShow.title
    }

    private fun displayTvShows(binding: ItemTvShowTvShowsBinding) {
        binding.root.apply {
            setOnClickListener {
                findNavController().navigate(
                    TvShowsFragmentDirections.actionTvShowsToTvShow(
                        id = tvShow.id
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
            .load(tvShow.poster)
            .centerCrop()
            .into(binding.ivTvShowPoster)

        binding.tvTvShowQuality.text = tvShow.quality?.name ?: "N/A"

        binding.tvTvShowLastEpisode.text =
            "S${tvShow.seasons.lastOrNull()?.number ?: ""} E${tvShow.seasons.lastOrNull()?.episodes?.lastOrNull()?.number ?: ""}"

        binding.tvTvShowTitle.text = tvShow.title
    }

    private fun displaySearch(binding: ItemTvShowSearchBinding) {
        binding.root.apply {
            setOnClickListener {
                findNavController().navigate(
                    SearchFragmentDirections.actionSearchToTvShow(
                        id = tvShow.id
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
            .load(tvShow.poster)
            .centerCrop()
            .into(binding.ivTvShowPoster)

        binding.tvTvShowQuality.text = tvShow.quality?.name ?: "N/A"

        binding.tvTvShowLastEpisode.text =
            "S${tvShow.seasons.lastOrNull()?.number ?: ""} E${tvShow.seasons.lastOrNull()?.episodes?.lastOrNull()?.number ?: ""}"

        binding.tvTvShowTitle.text = tvShow.title
    }


    private fun displayHeader(binding: ItemTvShowHeaderBinding) {
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

        binding.btnTvShowSeasons.apply {
            setOnClickListener {
                findNavController().navigate(
                    TvShowFragmentDirections.actionTvShowToSeasons(
                        tvShowId = tvShow.id,
                        tvShowTitle = tvShow.title,
                        tvShowBanner = tvShow.banner,
                    )
                )
            }
        }

        binding.btnTvShowTrailer.setOnClickListener {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.youtube.com/watch?v=${tvShow.youtubeTrailerId}")
                )
            )
        }
    }

    private fun displayCasts(binding: ItemTvShowCastsBinding) {
        binding.hgvTvShowPeoples.apply {
            setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
            adapter = SflixAdapter(tvShow.casts)
            setItemSpacing(80)
        }
    }
}