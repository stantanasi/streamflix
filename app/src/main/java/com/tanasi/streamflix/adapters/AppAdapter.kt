package com.tanasi.streamflix.adapters

import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.tanasi.streamflix.adapters.viewholders.CategoryViewHolder
import com.tanasi.streamflix.adapters.viewholders.EpisodeViewHolder
import com.tanasi.streamflix.adapters.viewholders.GenreViewHolder
import com.tanasi.streamflix.adapters.viewholders.LoadingViewHolder
import com.tanasi.streamflix.adapters.viewholders.MovieViewHolder
import com.tanasi.streamflix.adapters.viewholders.PeopleViewHolder
import com.tanasi.streamflix.adapters.viewholders.ProviderViewHolder
import com.tanasi.streamflix.adapters.viewholders.SeasonViewHolder
import com.tanasi.streamflix.adapters.viewholders.TvShowViewHolder
import com.tanasi.streamflix.databinding.ContentCategorySwiperBinding
import com.tanasi.streamflix.databinding.ContentMovieBinding
import com.tanasi.streamflix.databinding.ContentMovieCastsBinding
import com.tanasi.streamflix.databinding.ContentMovieRecommendationsBinding
import com.tanasi.streamflix.databinding.ContentTvShowBinding
import com.tanasi.streamflix.databinding.ContentTvShowCastsBinding
import com.tanasi.streamflix.databinding.ContentTvShowRecommendationsBinding
import com.tanasi.streamflix.databinding.ContentTvShowSeasonsBinding
import com.tanasi.streamflix.databinding.ItemCategoryBinding
import com.tanasi.streamflix.databinding.ItemEpisodeBinding
import com.tanasi.streamflix.databinding.ItemEpisodeContinueWatchingBinding
import com.tanasi.streamflix.databinding.ItemGenreGridBinding
import com.tanasi.streamflix.databinding.ItemLoadingBinding
import com.tanasi.streamflix.databinding.ItemMovieBinding
import com.tanasi.streamflix.databinding.ItemMovieContinueWatchingBinding
import com.tanasi.streamflix.databinding.ItemMovieGridBinding
import com.tanasi.streamflix.databinding.ItemPeopleBinding
import com.tanasi.streamflix.databinding.ItemProviderBinding
import com.tanasi.streamflix.databinding.ItemSeasonBinding
import com.tanasi.streamflix.databinding.ItemTvShowBinding
import com.tanasi.streamflix.databinding.ItemTvShowGridBinding
import com.tanasi.streamflix.models.Category
import com.tanasi.streamflix.models.Episode
import com.tanasi.streamflix.models.Genre
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.models.People
import com.tanasi.streamflix.models.Provider
import com.tanasi.streamflix.models.Season
import com.tanasi.streamflix.models.TvShow

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

        LOADING,

        MOVIE_ITEM,
        MOVIE_GRID_ITEM,
        MOVIE_CONTINUE_WATCHING_ITEM,

        MOVIE,
        MOVIE_CASTS,
        MOVIE_RECOMMENDATIONS,

        PEOPLE_ITEM,

        PROVIDER,

        SEASON_ITEM,

        TV_SHOW_ITEM,
        TV_SHOW_GRID_ITEM,

        TV_SHOW,
        TV_SHOW_SEASONS,
        TV_SHOW_CASTS,
        TV_SHOW_RECOMMENDATIONS,
    }

    private val states = mutableMapOf<Int, Parcelable?>()

    var isLoading = false
    private var onLoadMoreListener: (() -> Unit)? = null

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

            Type.LOADING -> LoadingViewHolder(
                ItemLoadingBinding.inflate(
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
        if (position >= itemCount - 5 && !isLoading) {
            onLoadMoreListener?.invoke()
            isLoading = true
        }

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

        val state = states[holder.layoutPosition]
        if (state != null) {
            when (holder) {
                is CategoryViewHolder -> holder.childRecyclerView?.layoutManager?.onRestoreInstanceState(state)
                is MovieViewHolder -> holder.childRecyclerView?.layoutManager?.onRestoreInstanceState(state)
                is TvShowViewHolder -> holder.childRecyclerView?.layoutManager?.onRestoreInstanceState(state)
            }
        }
    }

    override fun getItemCount(): Int = items.size + when {
        onLoadMoreListener != null -> 1
        else -> 0
    }

    override fun getItemViewType(position: Int): Int = items.getOrNull(position)?.itemType?.ordinal
        ?: Type.LOADING.ordinal

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)

        states[holder.layoutPosition] = when (holder) {
            is CategoryViewHolder -> holder.childRecyclerView?.layoutManager?.onSaveInstanceState()
            is MovieViewHolder -> holder.childRecyclerView?.layoutManager?.onSaveInstanceState()
            is TvShowViewHolder -> holder.childRecyclerView?.layoutManager?.onSaveInstanceState()
            else -> null
        }
    }

    fun onSaveInstanceState(recyclerView: RecyclerView) {
        for (position in items.indices) {
            val holder = recyclerView.findViewHolderForAdapterPosition(position) ?: continue

            states[position] = when (holder) {
                is CategoryViewHolder -> holder.childRecyclerView?.layoutManager?.onSaveInstanceState()
                is MovieViewHolder -> holder.childRecyclerView?.layoutManager?.onSaveInstanceState()
                is TvShowViewHolder -> holder.childRecyclerView?.layoutManager?.onSaveInstanceState()
                else -> null
            }
        }
    }


    fun submitList(list: List<Item>) {
        val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = items.size

            override fun getNewListSize() = list.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return items[oldItemPosition] === list[newItemPosition]
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return items[oldItemPosition] == list[newItemPosition]
            }
        })

        items.clear()
        items.addAll(list)
        result.dispatchUpdatesTo(this)
    }


    fun setOnLoadMoreListener(onLoadMoreListener: (() -> Unit)?) {
        this.onLoadMoreListener = onLoadMoreListener
        if (onLoadMoreListener == null) {
            notifyItemRemoved(items.size)
        }
    }
}