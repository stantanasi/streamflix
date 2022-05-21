package com.tanasi.sflix.presenters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.leanback.widget.Presenter
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.tanasi.sflix.databinding.ItemTvShowBinding
import com.tanasi.sflix.models.TvShow

class TvShowPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        return VhTvShow(
            ItemTvShowBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        when (viewHolder) {
            is VhTvShow -> viewHolder.bind(item as TvShow)
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
    }


    class VhTvShow(
        private val _binding: ViewBinding
    ) : ViewHolder(
        _binding.root
    ) {

        private val context = view.context
        private lateinit var tvShow: TvShow

        fun bind(tvShow: TvShow) {
            this.tvShow = tvShow

            when (_binding) {
                is ItemTvShowBinding -> displayTvShow(_binding)
            }
        }


        private fun displayTvShow(binding: ItemTvShowBinding) {
            Glide.with(context)
                .load(tvShow.poster)
                .centerCrop()
                .into(binding.ivTvShowPoster)

            binding.tvTvShowTitle.text = tvShow.title
        }
    }
}