package com.tanasi.sflix.adapters.view_holders

import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.tanasi.sflix.R
import com.tanasi.sflix.databinding.ItemTvShowBinding
import com.tanasi.sflix.models.TvShow

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
            is ItemTvShowBinding -> displayCard(_binding)
        }
    }


    private fun displayCard(binding: ItemTvShowBinding) {
        binding.root.setOnFocusChangeListener { _, hasFocus ->
            val animation = when {
                hasFocus -> AnimationUtils.loadAnimation(context, R.anim.zoom_in)
                else -> AnimationUtils.loadAnimation(context, R.anim.zoom_out)
            }
            binding.root.startAnimation(animation)
            animation.fillAfter = true
        }

        Glide.with(context)
            .load(tvShow.poster)
            .centerCrop()
            .into(binding.ivTvShowPoster)

        binding.tvTvShowTitle.text = tvShow.title
    }
}