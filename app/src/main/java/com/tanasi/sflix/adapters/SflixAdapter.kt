package com.tanasi.sflix.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tanasi.sflix.adapters.view_holders.*
import com.tanasi.sflix.databinding.*
import com.tanasi.sflix.models.*

class SflixAdapter(
    private val items: List<Item>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Item {
        var itemType: Type
    }

    enum class Type {
        EPISODE,

        MOVIE_HOME,
        MOVIE_MOVIES,
        MOVIE_SEARCH,

        MOVIE_HEADER,
        MOVIE_CASTS,

        PEOPLE,

        PEOPLE_HEADER,

        ROW,

        SEASON,

        TV_SHOW_HOME,
        TV_SHOW_TV_SHOWS,
        TV_SHOW_SEARCH,

        TV_SHOW_HEADER,
        TV_SHOW_CASTS,
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (Type.values()[viewType]) {
            Type.EPISODE -> VhEpisode(
                ItemEpisodeBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )

            Type.MOVIE_HOME -> VhMovie(
                ItemMovieHomeBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )
            Type.MOVIE_MOVIES -> VhMovie(
                ItemMovieMoviesBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )
            Type.MOVIE_SEARCH -> VhMovie(
                ItemMovieSearchBinding.inflate(
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
            Type.MOVIE_CASTS -> VhMovie(
                ItemMovieCastsBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )

            Type.PEOPLE -> VhPeople(
                ItemPeopleBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )

            Type.PEOPLE_HEADER -> VhPeople(
                ItemPeopleHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )

            Type.ROW -> VhRow(
                ItemRowBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )

            Type.SEASON -> VhSeason(
                ItemSeasonBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )

            Type.TV_SHOW_HOME -> VhTvShow(
                ItemTvShowHomeBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
            Type.TV_SHOW_TV_SHOWS -> VhTvShow(
                ItemTvShowTvShowsBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
            Type.TV_SHOW_SEARCH -> VhTvShow(
                ItemTvShowSearchBinding.inflate(
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
            Type.TV_SHOW_CASTS -> VhTvShow(
                ItemTvShowCastsBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is VhEpisode -> holder.bind(items[position] as Episode)
            is VhMovie -> holder.bind(items[position] as Movie)
            is VhPeople -> holder.bind(items[position] as People)
            is VhRow -> holder.bind(items[position] as Row)
            is VhSeason -> holder.bind(items[position] as Season)
            is VhTvShow -> holder.bind(items[position] as TvShow)
        }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int = items[position].itemType.ordinal
}