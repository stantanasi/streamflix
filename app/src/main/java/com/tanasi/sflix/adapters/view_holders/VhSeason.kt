package com.tanasi.sflix.adapters.view_holders

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.tanasi.sflix.adapters.SflixAdapter
import com.tanasi.sflix.databinding.ItemSeasonBinding
import com.tanasi.sflix.models.Season


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
        binding.tvSeasonNumber.text = season.title

        binding.hgvSeasonEpisodes.apply {
            setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
            adapter = SflixAdapter(season.episodes.map {
                it.apply { itemType = SflixAdapter.Type.EPISODE }
            })
        }
    }
}