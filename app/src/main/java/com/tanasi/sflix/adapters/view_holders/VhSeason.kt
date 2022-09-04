package com.tanasi.sflix.adapters.view_holders

import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.tanasi.sflix.R
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
        binding.root.apply {
            setOnFocusChangeListener { _, hasFocus ->
                val animation = when {
                    hasFocus -> AnimationUtils.loadAnimation(context, R.anim.zoom_in)
                    else -> AnimationUtils.loadAnimation(context, R.anim.zoom_out)
                }
                binding.root.startAnimation(animation)
                animation.fillAfter = true
            }
        }

        binding.tvSeasonTitle.text = season.title
    }
}