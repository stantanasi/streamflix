package com.tanasi.sflix.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tanasi.sflix.adapters.viewholders.*
import com.tanasi.sflix.databinding.*
import com.tanasi.sflix.models.*

class AppAdapter(
    val items: MutableList<Item> = mutableListOf()
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Item {
        var itemType: Type
    }

    enum class Type {
        CATEGORY_ITEM,

        CATEGORY_SWIPER,

        EPISODE_ITEM,
        EPISODE_CONTINUE_WATCHING_ITEM,

        GENRE_GRID_ITEM,

        MOVIE_ITEM,
        MOVIE_GRID_ITEM,
        MOVIE_CONTINUE_WATCHING_ITEM,

        MOVIE,
        MOVIE_CASTS,
        MOVIE_RECOMMENDATIONS,

        PEOPLE_ITEM,

        PEOPLE,

        PROVIDER,

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
            Type.CATEGORY_ITEM -> CategoryViewHolder(
                ItemCategoryBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )

            Type.CATEGORY_SWIPER -> CategoryViewHolder(
                ContentCategorySwiperBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )

            Type.EPISODE_ITEM -> EpisodeViewHolder(
                ItemEpisodeBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )
            Type.EPISODE_CONTINUE_WATCHING_ITEM -> EpisodeViewHolder(
                ItemEpisodeContinueWatchingBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )

            Type.GENRE_GRID_ITEM -> GenreViewHolder(
                ItemGenreGridBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )

            Type.MOVIE_ITEM -> MovieViewHolder(
                ItemMovieBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )
            Type.MOVIE_GRID_ITEM -> MovieViewHolder(
                ItemMovieGridBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )
            Type.MOVIE_CONTINUE_WATCHING_ITEM -> MovieViewHolder(
                ItemMovieContinueWatchingBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )

            Type.MOVIE -> MovieViewHolder(
                ContentMovieBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )
            Type.MOVIE_CASTS -> MovieViewHolder(
                ContentMovieCastsBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )
            Type.MOVIE_RECOMMENDATIONS -> MovieViewHolder(
                ContentMovieRecommendationsBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )

            Type.PEOPLE_ITEM -> PeopleViewHolder(
                ItemPeopleBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )

            Type.PEOPLE -> PeopleViewHolder(
                ContentPeopleBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )

            Type.PROVIDER -> ProviderViewHolder(
                ItemProviderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )

            Type.SEASON_ITEM -> SeasonViewHolder(
                ItemSeasonBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )

            Type.TV_SHOW_ITEM -> TvShowViewHolder(
                ItemTvShowBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
            Type.TV_SHOW_GRID_ITEM -> TvShowViewHolder(
                ItemTvShowGridBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

            Type.TV_SHOW -> TvShowViewHolder(
                ContentTvShowBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )
            Type.TV_SHOW_SEASONS -> TvShowViewHolder(
                ContentTvShowSeasonsBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )
            Type.TV_SHOW_CASTS -> TvShowViewHolder(
                ContentTvShowCastsBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )
            Type.TV_SHOW_RECOMMENDATIONS -> TvShowViewHolder(
                ContentTvShowRecommendationsBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CategoryViewHolder -> holder.bind(items[position] as Category)
            is EpisodeViewHolder -> holder.bind(items[position] as Episode)
            is GenreViewHolder -> holder.bind(items[position] as Genre)
            is MovieViewHolder -> holder.bind(items[position] as Movie)
            is PeopleViewHolder -> holder.bind(items[position] as People)
            is ProviderViewHolder -> holder.bind(items[position] as Provider)
            is SeasonViewHolder -> holder.bind(items[position] as Season)
            is TvShowViewHolder -> holder.bind(items[position] as TvShow)
        }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int = items[position].itemType.ordinal
}