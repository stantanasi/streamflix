package com.tanasi.sflix.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tanasi.sflix.adapters.view_holders.*
import com.tanasi.sflix.databinding.*
import com.tanasi.sflix.models.*

class SflixAdapter(
    val items: MutableList<Item> = mutableListOf()
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Item {
        var itemType: Type
    }

    enum class Type {
        EPISODE_ITEM,

        MOVIE_ITEM,
        MOVIE_GRID_ITEM,

        MOVIE,
        MOVIE_CASTS,
        MOVIE_RECOMMENDATIONS,

        PEOPLE_ITEM,

        PEOPLE,

        CATEGORY_ITEM,

        CATEGORY_SWIPER,

        SEASON_ITEM,

        TV_SHOW_ITEM,
        TV_SHOW_GRID_ITEM,

        TV_SHOW,
        TV_SHOW_SEASONS,
        TV_SHOW_CASTS,
        TV_SHOW_RECOMMENDATIONS,
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (Type.values()[viewType]) {
            Type.EPISODE_ITEM -> VhEpisode(
                ItemEpisodeBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )

            Type.MOVIE_ITEM -> VhMovie(
                ItemMovieBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )
            Type.MOVIE_GRID_ITEM -> VhMovie(
                ItemMovieGridBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )

            Type.MOVIE -> VhMovie(
                ContentMovieBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )
            Type.MOVIE_CASTS -> VhMovie(
                ContentMovieCastsBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )
            Type.MOVIE_RECOMMENDATIONS -> VhMovie(
                ContentMovieRecommendationsBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )

            Type.PEOPLE_ITEM -> VhPeople(
                ItemPeopleBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )

            Type.PEOPLE -> VhPeople(
                ContentPeopleBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )

            Type.CATEGORY_ITEM -> VhCategory(
                ItemCategoryBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )

            Type.CATEGORY_SWIPER -> VhCategory(
                ContentCategorySwiperBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )

            Type.SEASON_ITEM -> VhSeason(
                ItemSeasonBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )

            Type.TV_SHOW_ITEM -> VhTvShow(
                ItemTvShowBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
            Type.TV_SHOW_GRID_ITEM -> VhTvShow(
                ItemTvShowGridBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

            Type.TV_SHOW -> VhTvShow(
                ContentTvShowBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )
            Type.TV_SHOW_SEASONS -> VhTvShow(
                ContentTvShowSeasonsBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )
            Type.TV_SHOW_CASTS -> VhTvShow(
                ContentTvShowCastsBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )
            Type.TV_SHOW_RECOMMENDATIONS -> VhTvShow(
                ContentTvShowRecommendationsBinding.inflate(
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
            is VhCategory -> holder.bind(items[position] as Category)
            is VhSeason -> holder.bind(items[position] as Season)
            is VhTvShow -> holder.bind(items[position] as TvShow)
        }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int = items[position].itemType.ordinal
}