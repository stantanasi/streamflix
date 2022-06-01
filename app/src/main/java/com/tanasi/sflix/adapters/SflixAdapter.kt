package com.tanasi.sflix.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tanasi.sflix.adapters.view_holders.VhMovie
import com.tanasi.sflix.adapters.view_holders.VhTvShow
import com.tanasi.sflix.databinding.ItemMovieBinding
import com.tanasi.sflix.databinding.ItemMovieHeaderBinding
import com.tanasi.sflix.databinding.ItemTvShowBinding
import com.tanasi.sflix.databinding.ItemTvShowHeaderBinding
import com.tanasi.sflix.models.Movie
import com.tanasi.sflix.models.TvShow

class SflixAdapter(
    private val items: List<Item>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Item {
        var itemType: Type
    }

    enum class Type {
        MOVIE,
        MOVIE_HEADER,

        TV_SHOW,
        TV_SHOW_HEADER,
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (Type.values()[viewType]) {
            Type.MOVIE -> VhMovie(
                ItemMovieBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )
            Type.MOVIE_HEADER -> VhMovie(
                ItemMovieHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )

            Type.TV_SHOW -> VhTvShow(
                ItemTvShowBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
            Type.TV_SHOW_HEADER -> VhTvShow(
                ItemTvShowHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is VhMovie -> holder.bind(items[position] as Movie)
            is VhTvShow -> holder.bind(items[position] as TvShow)
        }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int = items[position].itemType.ordinal
}