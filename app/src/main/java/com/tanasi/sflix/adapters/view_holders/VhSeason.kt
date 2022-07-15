package com.tanasi.sflix.adapters.view_holders

import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.tanasi.sflix.databinding.ItemSeasonBinding
import com.tanasi.sflix.fragments.seasons.SeasonsFragment
import com.tanasi.sflix.models.Season
import com.tanasi.sflix.utils.getCurrentFragment
import com.tanasi.sflix.utils.toActivity


class VhSeason(
    private val _binding: ViewBinding
) : RecyclerView.ViewHolder(
    _binding.root
) {

    private val context = itemView.context
    private lateinit var season: Season

    fun bind(season: Season) {
        this.season = season

        when (_binding) {
            is ItemSeasonBinding -> displaySeason(_binding)
        }
    }


    private fun displaySeason(binding: ItemSeasonBinding) {
        binding.tvSeasonNumber.apply {
            text = season.title
            setOnClickListener {
                when (val fragment = context.toActivity()?.getCurrentFragment()) {
                    is SeasonsFragment -> fragment.viewModel.getSeasonEpisodes(season.id)
                }
            }
        }
    }
}