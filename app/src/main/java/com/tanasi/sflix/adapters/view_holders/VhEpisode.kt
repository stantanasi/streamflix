package com.tanasi.sflix.adapters.view_holders

import android.view.animation.AnimationUtils
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.tanasi.sflix.R
import com.tanasi.sflix.databinding.ItemEpisodeBinding
import com.tanasi.sflix.fragments.tv_show.TvShowFragmentDirections
import com.tanasi.sflix.models.Episode

class VhEpisode(
    private val _binding: ViewBinding
) : RecyclerView.ViewHolder(
    _binding.root
) {

    private val context = itemView.context
    private lateinit var episode: Episode

    fun bind(episode: Episode) {
        this.episode = episode

        when (_binding) {
            is ItemEpisodeBinding -> displayEpisode(_binding)
        }
    }


    private fun displayEpisode(binding: ItemEpisodeBinding) {
        binding.root.apply {
            setOnClickListener {
                findNavController().navigate(
                    TvShowFragmentDirections.actionTvShowToPlayer(
                        linkId = episode.servers.first().id,
                        title = "",
//                        description = tvShow.seasons.find { it.episodes.contains(item) }!!.let {
//                            "S${it.number} E${item.number}: ${item.title}"
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
            .load(episode.poster)
            .centerCrop()
            .into(binding.ivEpisodePoster)

        binding.tvEpisodeInfo.text = "Episode ${episode.number}"

        binding.tvEpisodeTitle.text = episode.title
    }
}