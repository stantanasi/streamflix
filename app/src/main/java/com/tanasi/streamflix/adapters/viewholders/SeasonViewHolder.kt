package com.tanasi.streamflix.adapters.viewholders

import android.view.animation.AnimationUtils
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.tanasi.streamflix.R
import com.tanasi.streamflix.databinding.ItemSeasonBinding
import com.tanasi.streamflix.databinding.ItemSeasonMobileBinding
import com.tanasi.streamflix.fragments.tv_show.TvShowFragmentDirections
import com.tanasi.streamflix.fragments.tv_show.TvShowMobileFragmentDirections
import com.tanasi.streamflix.models.Season


class SeasonViewHolder(
    private val _binding: ViewBinding
) : RecyclerView.ViewHolder(
    _binding.root
) {

    private val context = itemView.context
    private lateinit var season: Season

    fun bind(season: Season) {
        this.season = season

        when (_binding) {
            is ItemSeasonMobileBinding -> displayMobileItem(_binding)
            is ItemSeasonBinding -> displayItem(_binding)
        }
    }


    private fun displayMobileItem(binding: ItemSeasonMobileBinding) {
        binding.root.apply {
            setOnClickListener {
                findNavController().navigate(
                    TvShowMobileFragmentDirections.actionTvShowToSeason(
                        tvShowId = season.tvShow?.id ?: "",
                        tvShowTitle = season.tvShow?.title ?: "",
                        tvShowPoster = season.tvShow?.poster,
                        tvShowBanner = season.tvShow?.banner,
                        seasonId = season.id,
                        seasonNumber = season.number,
                        seasonTitle = season.title,
                    )
                )
            }
        }

        binding.ivSeasonPoster.apply {
            clipToOutline = true
            Glide.with(context)
                .load(season.poster)
                .centerCrop()
                .into(this)
        }

        binding.tvSeasonTitle.text = season.title
    }

    private fun displayItem(binding: ItemSeasonBinding) {
        binding.root.apply {
            setOnClickListener {
                findNavController().navigate(
                    TvShowFragmentDirections.actionTvShowToSeason(
                        tvShowId = season.tvShow?.id ?: "",
                        tvShowTitle = season.tvShow?.title ?: "",
                        tvShowPoster = season.tvShow?.poster,
                        tvShowBanner = season.tvShow?.banner,
                        seasonId = season.id,
                        seasonNumber = season.number,
                        seasonTitle = season.title,
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

        binding.ivSeasonPoster.apply {
            clipToOutline = true
            Glide.with(context)
                .load(season.poster)
                .centerCrop()
                .into(this)
        }

        binding.tvSeasonTitle.text = season.title
    }
}