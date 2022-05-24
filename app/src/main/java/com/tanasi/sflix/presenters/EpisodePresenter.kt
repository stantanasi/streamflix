package com.tanasi.sflix.presenters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.leanback.widget.Presenter
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.tanasi.sflix.databinding.ItemEpisodeBinding
import com.tanasi.sflix.models.Episode

class EpisodePresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        return VhEpisode(
            ItemEpisodeBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        when (viewHolder) {
            is VhEpisode -> viewHolder.bind(item as Episode)
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
    }

    class VhEpisode(
        private val _binding: ViewBinding
    ) : ViewHolder(
        _binding.root
    ) {

        private val context = view.context
        private lateinit var episode: Episode

        fun bind(episode: Episode) {
            this.episode = episode

            when (_binding) {
                is ItemEpisodeBinding -> displayTvShow(_binding)
            }
        }


        private fun displayTvShow(binding: ItemEpisodeBinding) {
            Glide.with(context)
                .load(episode.poster)
                .centerCrop()
                .into(binding.ivEpisodePoster)

            binding.tvEpisodeInfo.text = "Episode ${episode.number}"

            binding.tvEpisodeTitle.text = episode.title
        }
    }
}