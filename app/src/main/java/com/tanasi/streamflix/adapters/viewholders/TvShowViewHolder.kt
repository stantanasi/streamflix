package com.tanasi.streamflix.adapters.viewholders

import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.tanasi.streamflix.R
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.database.AppDatabase
import com.tanasi.streamflix.databinding.ContentTvShowCastMobileBinding
import com.tanasi.streamflix.databinding.ContentTvShowCastTvBinding
import com.tanasi.streamflix.databinding.ContentTvShowMobileBinding
import com.tanasi.streamflix.databinding.ContentTvShowRecommendationsMobileBinding
import com.tanasi.streamflix.databinding.ContentTvShowRecommendationsTvBinding
import com.tanasi.streamflix.databinding.ContentTvShowSeasonsMobileBinding
import com.tanasi.streamflix.databinding.ContentTvShowSeasonsTvBinding
import com.tanasi.streamflix.databinding.ContentTvShowTvBinding
import com.tanasi.streamflix.databinding.ItemCategorySwiperMobileBinding
import com.tanasi.streamflix.databinding.ItemTvShowGridBinding
import com.tanasi.streamflix.databinding.ItemTvShowGridMobileBinding
import com.tanasi.streamflix.databinding.ItemTvShowMobileBinding
import com.tanasi.streamflix.databinding.ItemTvShowTvBinding
import com.tanasi.streamflix.fragments.genre.GenreMobileFragment
import com.tanasi.streamflix.fragments.genre.GenreMobileFragmentDirections
import com.tanasi.streamflix.fragments.genre.GenreTvFragment
import com.tanasi.streamflix.fragments.genre.GenreTvFragmentDirections
import com.tanasi.streamflix.fragments.home.HomeMobileFragment
import com.tanasi.streamflix.fragments.home.HomeMobileFragmentDirections
import com.tanasi.streamflix.fragments.home.HomeTvFragment
import com.tanasi.streamflix.fragments.home.HomeTvFragmentDirections
import com.tanasi.streamflix.fragments.movie.MovieMobileFragment
import com.tanasi.streamflix.fragments.movie.MovieMobileFragmentDirections
import com.tanasi.streamflix.fragments.movie.MovieTvFragment
import com.tanasi.streamflix.fragments.movie.MovieTvFragmentDirections
import com.tanasi.streamflix.fragments.people.PeopleMobileFragment
import com.tanasi.streamflix.fragments.people.PeopleMobileFragmentDirections
import com.tanasi.streamflix.fragments.people.PeopleTvFragment
import com.tanasi.streamflix.fragments.people.PeopleTvFragmentDirections
import com.tanasi.streamflix.fragments.search.SearchMobileFragment
import com.tanasi.streamflix.fragments.search.SearchMobileFragmentDirections
import com.tanasi.streamflix.fragments.search.SearchTvFragment
import com.tanasi.streamflix.fragments.search.SearchTvFragmentDirections
import com.tanasi.streamflix.fragments.tv_show.TvShowMobileFragment
import com.tanasi.streamflix.fragments.tv_show.TvShowMobileFragmentDirections
import com.tanasi.streamflix.fragments.tv_show.TvShowTvFragment
import com.tanasi.streamflix.fragments.tv_show.TvShowTvFragmentDirections
import com.tanasi.streamflix.fragments.tv_shows.TvShowsMobileFragment
import com.tanasi.streamflix.fragments.tv_shows.TvShowsMobileFragmentDirections
import com.tanasi.streamflix.fragments.tv_shows.TvShowsTvFragment
import com.tanasi.streamflix.fragments.tv_shows.TvShowsTvFragmentDirections
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.models.Video
import com.tanasi.streamflix.ui.ShowOptionsMobileDialog
import com.tanasi.streamflix.ui.ShowOptionsTvDialog
import com.tanasi.streamflix.ui.SpacingItemDecoration
import com.tanasi.streamflix.utils.dp
import com.tanasi.streamflix.utils.format
import com.tanasi.streamflix.utils.getCurrentFragment
import com.tanasi.streamflix.utils.toActivity
import java.util.Locale

class TvShowViewHolder(
    private val _binding: ViewBinding
) : RecyclerView.ViewHolder(
    _binding.root
) {

    private val context = itemView.context
    private val database = AppDatabase.getInstance(context)
    private lateinit var tvShow: TvShow

    val childRecyclerView: RecyclerView?
        get() = when (_binding) {
            is ContentTvShowSeasonsMobileBinding -> _binding.rvTvShowSeasons
            is ContentTvShowSeasonsTvBinding -> _binding.hgvTvShowSeasons
            is ContentTvShowCastMobileBinding -> _binding.rvTvShowCast
            is ContentTvShowCastTvBinding -> _binding.hgvTvShowCast
            is ContentTvShowRecommendationsMobileBinding -> _binding.rvTvShowRecommendations
            is ContentTvShowRecommendationsTvBinding -> _binding.hgvTvShowRecommendations
            else -> null
        }

    fun bind(tvShow: TvShow) {
        this.tvShow = tvShow

        when (_binding) {
            is ItemTvShowMobileBinding -> displayMobileItem(_binding)
            is ItemTvShowTvBinding -> displayTvItem(_binding)
            is ItemTvShowGridMobileBinding -> displayGridMobileItem(_binding)
            is ItemTvShowGridBinding -> displayGridTvItem(_binding)
            is ItemCategorySwiperMobileBinding -> displaySwiperMobileItem(_binding)

            is ContentTvShowMobileBinding -> displayTvShowMobile(_binding)
            is ContentTvShowTvBinding -> displayTvShowTv(_binding)
            is ContentTvShowSeasonsMobileBinding -> displaySeasonsMobile(_binding)
            is ContentTvShowSeasonsTvBinding -> displaySeasonsTv(_binding)
            is ContentTvShowCastMobileBinding -> displayCastMobile(_binding)
            is ContentTvShowCastTvBinding -> displayCastTv(_binding)
            is ContentTvShowRecommendationsMobileBinding -> displayRecommendationsMobile(_binding)
            is ContentTvShowRecommendationsTvBinding -> displayRecommendationsTv(_binding)
        }
    }


    private fun displayMobileItem(binding: ItemTvShowMobileBinding) {
        binding.root.apply {
            setOnClickListener {
                when (context.toActivity()?.getCurrentFragment()) {
                    is HomeMobileFragment -> findNavController().navigate(
                        HomeMobileFragmentDirections.actionHomeToTvShow(
                            id = tvShow.id
                        )
                    )
                    is MovieMobileFragment -> findNavController().navigate(
                        MovieMobileFragmentDirections.actionMovieToTvShow(
                            id = tvShow.id
                        )
                    )
                    is TvShowMobileFragment -> findNavController().navigate(
                        TvShowMobileFragmentDirections.actionTvShowToTvShow(
                            id = tvShow.id
                        )
                    )
                }
            }
            setOnLongClickListener {
                ShowOptionsMobileDialog(context, tvShow)
                    .show()
                true
            }
        }

        Glide.with(context)
            .load(tvShow.poster)
            .centerCrop()
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.ivTvShowPoster)

        binding.tvTvShowQuality.apply {
            text = tvShow.quality ?: ""
            visibility = when {
                tvShow.quality.isNullOrEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.tvTvShowLastEpisode.text = tvShow.seasons.lastOrNull()?.let { season ->
            season.episodes.lastOrNull()?.let { episode ->
                if (season.number != 0) {
                    context.getString(
                        R.string.tv_show_item_season_number_episode_number,
                        season.number,
                        episode.number
                    )
                } else {
                    context.getString(
                        R.string.tv_show_item_episode_number,
                        episode.number
                    )
                }
            }
        } ?: context.getString(R.string.tv_show_item_type)

        binding.tvTvShowTitle.text = tvShow.title
    }

    private fun displayTvItem(binding: ItemTvShowTvBinding) {
        binding.root.apply {
            setOnClickListener {
                when (context.toActivity()?.getCurrentFragment()) {
                    is HomeTvFragment -> findNavController().navigate(
                        HomeTvFragmentDirections.actionHomeToTvShow(
                            id = tvShow.id
                        )
                    )
                    is MovieTvFragment -> findNavController().navigate(
                        MovieTvFragmentDirections.actionMovieToTvShow(
                            id = tvShow.id
                        )
                    )
                    is TvShowTvFragment -> findNavController().navigate(
                        TvShowTvFragmentDirections.actionTvShowToTvShow(
                            id = tvShow.id
                        )
                    )
                }
            }
            setOnLongClickListener {
                ShowOptionsTvDialog(context, tvShow)
                    .show()
                true
            }
            setOnFocusChangeListener { _, hasFocus ->
                val animation = when {
                    hasFocus -> AnimationUtils.loadAnimation(context, R.anim.zoom_in)
                    else -> AnimationUtils.loadAnimation(context, R.anim.zoom_out)
                }
                binding.root.startAnimation(animation)
                animation.fillAfter = true

                if (hasFocus) {
                    when (val fragment = context.toActivity()?.getCurrentFragment()) {
                        is HomeTvFragment -> fragment.updateBackground(tvShow.banner)
                    }
                }
            }
        }

        Glide.with(context)
            .load(tvShow.poster)
            .fallback(R.drawable.glide_fallback_cover)
            .centerCrop()
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.ivTvShowPoster)

        binding.tvTvShowQuality.apply {
            text = tvShow.quality ?: ""
            visibility = when {
                tvShow.quality.isNullOrEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.tvTvShowLastEpisode.text = tvShow.seasons.lastOrNull()?.let { season ->
            season.episodes.lastOrNull()?.let { episode ->
                if (season.number != 0) {
                    context.getString(
                        R.string.tv_show_item_season_number_episode_number,
                        season.number,
                        episode.number
                    )
                } else {
                    context.getString(
                        R.string.tv_show_item_episode_number,
                        episode.number
                    )
                }
            }
        } ?: context.getString(R.string.tv_show_item_type)

        binding.tvTvShowTitle.text = tvShow.title
    }

    private fun displayGridMobileItem(binding: ItemTvShowGridMobileBinding) {
        binding.root.apply {
            setOnClickListener {
                when (context.toActivity()?.getCurrentFragment()) {
                    is GenreMobileFragment -> findNavController().navigate(
                        GenreMobileFragmentDirections.actionGenreToTvShow(
                            id = tvShow.id
                        )
                    )
                    is PeopleMobileFragment -> findNavController().navigate(
                        PeopleMobileFragmentDirections.actionPeopleToTvShow(
                            id = tvShow.id
                        )
                    )
                    is SearchMobileFragment -> findNavController().navigate(
                        SearchMobileFragmentDirections.actionSearchToTvShow(
                            id = tvShow.id
                        )
                    )
                    is TvShowsMobileFragment -> findNavController().navigate(
                        TvShowsMobileFragmentDirections.actionTvShowsToTvShow(
                            id = tvShow.id
                        )
                    )
                }
            }
            setOnLongClickListener {
                ShowOptionsMobileDialog(context, tvShow)
                    .show()
                true
            }
        }

        Glide.with(context)
            .load(tvShow.poster)
            .centerCrop()
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.ivTvShowPoster)

        binding.tvTvShowQuality.apply {
            text = tvShow.quality ?: ""
            visibility = when {
                tvShow.quality.isNullOrEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.tvTvShowLastEpisode.text = tvShow.seasons.lastOrNull()?.let { season ->
            season.episodes.lastOrNull()?.let { episode ->
                if (season.number != 0) {
                    context.getString(
                        R.string.tv_show_item_season_number_episode_number,
                        season.number,
                        episode.number
                    )
                } else {
                    context.getString(
                        R.string.tv_show_item_episode_number,
                        episode.number
                    )
                }
            }
        } ?: context.getString(R.string.tv_show_item_type)

        binding.tvTvShowTitle.text = tvShow.title
    }

    private fun displayGridTvItem(binding: ItemTvShowGridBinding) {
        binding.root.apply {
            setOnClickListener {
                when (context.toActivity()?.getCurrentFragment()) {
                    is GenreTvFragment -> findNavController().navigate(
                        GenreTvFragmentDirections.actionGenreToTvShow(
                            id = tvShow.id
                        )
                    )
                    is PeopleTvFragment -> findNavController().navigate(
                        PeopleTvFragmentDirections.actionPeopleToTvShow(
                            id = tvShow.id
                        )
                    )
                    is SearchTvFragment -> findNavController().navigate(
                        SearchTvFragmentDirections.actionSearchToTvShow(
                            id = tvShow.id
                        )
                    )
                    is TvShowsTvFragment -> findNavController().navigate(
                        TvShowsTvFragmentDirections.actionTvShowsToTvShow(
                            id = tvShow.id
                        )
                    )
                }
            }
            setOnLongClickListener {
                ShowOptionsTvDialog(context, tvShow)
                    .show()
                true
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
            .load(tvShow.poster)
            .fallback(R.drawable.glide_fallback_cover)
            .centerCrop()
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.ivTvShowPoster)

        binding.tvTvShowQuality.apply {
            text = tvShow.quality ?: ""
            visibility = when {
                tvShow.quality.isNullOrEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.tvTvShowLastEpisode.text = tvShow.seasons.lastOrNull()?.let { season ->
            season.episodes.lastOrNull()?.let { episode ->
                if (season.number != 0) {
                    context.getString(
                        R.string.tv_show_item_season_number_episode_number,
                        season.number,
                        episode.number
                    )
                } else {
                    context.getString(
                        R.string.tv_show_item_episode_number,
                        episode.number
                    )
                }
            }
        } ?: context.getString(R.string.tv_show_item_type)

        binding.tvTvShowTitle.text = tvShow.title
    }


    private fun displaySwiperMobileItem(binding: ItemCategorySwiperMobileBinding) {
        Glide.with(context)
            .load(tvShow.banner)
            .centerCrop()
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.ivSwiperBackground)

        binding.tvSwiperTitle.text = tvShow.title

        binding.tvSwiperTvShowLastEpisode.apply {
            text = tvShow.seasons.lastOrNull()?.let { season ->
                season.episodes.lastOrNull()?.let { episode ->
                    if (season.number != 0) {
                        context.getString(
                            R.string.tv_show_item_season_number_episode_number,
                            season.number,
                            episode.number
                        )
                    } else {
                        context.getString(
                            R.string.tv_show_item_episode_number,
                            episode.number
                        )
                    }
                }
            } ?: context.getString(R.string.tv_show_item_type)
        }

        binding.tvSwiperQuality.apply {
            text = tvShow.quality
            visibility = when {
                text.isNullOrEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.tvSwiperReleased.apply {
            text = tvShow.released?.format("yyyy")
            visibility = when {
                text.isNullOrEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.tvSwiperRating.apply {
            text = tvShow.rating?.let { String.format(Locale.ROOT, "%.1f", it) } ?: "N/A"
            visibility = when {
                text.isNullOrEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.ivSwiperRatingIcon.visibility = binding.tvSwiperRating.visibility

        binding.tvSwiperOverview.apply {
            setOnClickListener {
                maxLines = when (maxLines) {
                    2 -> Int.MAX_VALUE
                    else -> 2
                }
            }

            text = tvShow.overview
        }

        binding.btnSwiperWatchNow.apply {
            setOnClickListener {
                handler.removeCallbacksAndMessages(null)
                findNavController().navigate(
                    HomeMobileFragmentDirections.actionHomeToTvShow(
                        id = tvShow.id,
                    )
                )
            }
        }

        binding.pbSwiperProgress.visibility = View.GONE
    }


    private fun displayTvShowMobile(binding: ContentTvShowMobileBinding) {
        binding.ivTvShowPoster.run {
            Glide.with(context)
                .load(tvShow.poster)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(this)
            visibility = when {
                tvShow.poster.isNullOrEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.tvTvShowTitle.text = tvShow.title

        binding.tvTvShowRating.text = tvShow.rating?.let { String.format(Locale.ROOT, "%.1f", it) } ?: "N/A"

        binding.tvTvShowQuality.apply {
            text = tvShow.quality
            visibility = when {
                text.isNullOrEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.tvTvShowReleased.apply {
            text = tvShow.released?.format("yyyy")
            visibility = when {
                text.isNullOrEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.tvTvShowRuntime.apply {
            text = tvShow.runtime?.let {
                val hours = it / 60
                val minutes = it % 60
                when {
                    hours > 0 -> context.getString(
                        R.string.tv_show_runtime_hours_minutes,
                        hours,
                        minutes
                    )
                    else -> context.getString(R.string.tv_show_runtime_minutes, minutes)
                }
            }
            visibility = when {
                text.isNullOrEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.tvTvShowGenres.apply {
            text = tvShow.genres.joinToString(", ") { it.name }
            visibility = when {
                tvShow.genres.isEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.tvTvShowOverview.text = tvShow.overview

        val episodeToWatch = tvShow.episodeToWatch

        binding.btnTvShowWatchEpisode.apply {
            setOnClickListener {
                if (episodeToWatch == null) return@setOnClickListener

                findNavController().navigate(
                    TvShowMobileFragmentDirections.actionTvShowToPlayer(
                        id = episodeToWatch.id,
                        title = tvShow.title,
                        subtitle = episodeToWatch.season?.takeIf { it.number != 0 }?.let { season ->
                            context.getString(
                                R.string.player_subtitle_tv_show,
                                season.number,
                                episodeToWatch.number,
                                episodeToWatch.title ?: context.getString(
                                    R.string.episode_number,
                                    episodeToWatch.number
                                )
                            )
                        } ?: context.getString(
                            R.string.player_subtitle_tv_show_episode_only,
                            episodeToWatch.number,
                            episodeToWatch.title ?: context.getString(
                                R.string.episode_number,
                                episodeToWatch.number
                            )
                        ),
                        videoType = Video.Type.Episode(
                            id = episodeToWatch.id,
                            number = episodeToWatch.number,
                            title = episodeToWatch.title,
                            poster = episodeToWatch.poster,
                            tvShow = Video.Type.Episode.TvShow(
                                id = tvShow.id,
                                title = tvShow.title,
                                poster = tvShow.poster,
                                banner = tvShow.banner,
                            ),
                            season = Video.Type.Episode.Season(
                                number = episodeToWatch.season?.number ?: 0,
                                title = episodeToWatch.season?.title,
                            ),
                        ),
                    )
                )
            }

            text = if (episodeToWatch != null) {
                episodeToWatch.season?.takeIf { it.number != 0 }?.let { season ->
                    context.getString(
                        R.string.tv_show_watch_season_episode,
                        season.number,
                        episodeToWatch.number
                    )
                } ?: context.getString(
                    R.string.tv_show_watch_episode,
                    episodeToWatch.number
                )
            } else ""
            visibility = when {
                episodeToWatch != null -> View.VISIBLE
                else -> View.GONE
            }
        }

        binding.pbTvShowWatchEpisodeLoading.apply {
            visibility = when {
                episodeToWatch != null -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.pbTvShowProgressEpisode.apply {
            val watchHistory = episodeToWatch?.watchHistory

            progress = when {
                watchHistory != null -> (watchHistory.lastPlaybackPositionMillis * 100 / watchHistory.durationMillis.toDouble()).toInt()
                else -> 0
            }
            visibility = when {
                watchHistory != null -> View.VISIBLE
                else -> View.GONE
            }
        }

        binding.btnTvShowTrailer.apply {
            val trailer = tvShow.trailer

            setOnClickListener {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(trailer)
                    )
                )
            }

            visibility = when {
                trailer != null -> View.VISIBLE
                else -> View.GONE
            }
        }

        binding.btnTvShowFavorite.apply {
            fun Boolean.drawable() = when (this) {
                true -> R.drawable.ic_favorite_enable
                false -> R.drawable.ic_favorite_disable
            }

            setOnClickListener {
                tvShow.isFavorite = !tvShow.isFavorite
                database.tvShowDao().update(tvShow)

                setImageDrawable(
                    ContextCompat.getDrawable(context, tvShow.isFavorite.drawable())
                )
            }

            setImageDrawable(
                ContextCompat.getDrawable(context, tvShow.isFavorite.drawable())
            )
        }
    }

    private fun displayTvShowTv(binding: ContentTvShowTvBinding) {
        binding.ivTvShowPoster.run {
            Glide.with(context)
                .load(tvShow.poster)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(this)
            visibility = when {
                tvShow.poster.isNullOrEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.tvTvShowTitle.text = tvShow.title

        binding.tvTvShowRating.text = tvShow.rating?.let { String.format(Locale.ROOT, "%.1f", it) } ?: "N/A"

        binding.tvTvShowQuality.apply {
            text = tvShow.quality
            visibility = when {
                text.isNullOrEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.tvTvShowReleased.apply {
            text = tvShow.released?.format("yyyy")
            visibility = when {
                text.isNullOrEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.tvTvShowRuntime.apply {
            text = tvShow.runtime?.let {
                val hours = it / 60
                val minutes = it % 60
                when {
                    hours > 0 -> context.getString(
                        R.string.tv_show_runtime_hours_minutes,
                        hours,
                        minutes
                    )
                    else -> context.getString(R.string.tv_show_runtime_minutes, minutes)
                }
            }
            visibility = when {
                text.isNullOrEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.tvTvShowGenres.apply {
            text = tvShow.genres.joinToString(", ") { it.name }
            visibility = when {
                tvShow.genres.isEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.tvTvShowOverview.text = tvShow.overview

        val episodeToWatch = tvShow.episodeToWatch

        binding.btnTvShowWatchEpisode.apply {
            setOnClickListener {
                if (episodeToWatch == null) return@setOnClickListener

                findNavController().navigate(
                    TvShowTvFragmentDirections.actionTvShowToPlayer(
                        id = episodeToWatch.id,
                        title = tvShow.title,
                        subtitle = episodeToWatch.season?.takeIf { it.number != 0 }?.let { season ->
                            context.getString(
                                R.string.player_subtitle_tv_show,
                                season.number,
                                episodeToWatch.number,
                                episodeToWatch.title ?: context.getString(
                                    R.string.episode_number,
                                    episodeToWatch.number
                                )
                            )
                        } ?: context.getString(
                            R.string.player_subtitle_tv_show_episode_only,
                            episodeToWatch.number,
                            episodeToWatch.title ?: context.getString(
                                R.string.episode_number,
                                episodeToWatch.number
                            )
                        ),
                        videoType = Video.Type.Episode(
                            id = episodeToWatch.id,
                            number = episodeToWatch.number,
                            title = episodeToWatch.title,
                            poster = episodeToWatch.poster,
                            tvShow = Video.Type.Episode.TvShow(
                                id = tvShow.id,
                                title = tvShow.title,
                                poster = tvShow.poster,
                                banner = tvShow.banner,
                            ),
                            season = Video.Type.Episode.Season(
                                number = episodeToWatch.season?.number ?: 0,
                                title = episodeToWatch.season?.title,
                            ),
                        ),
                    )
                )
            }

            text = if (episodeToWatch != null) {
                episodeToWatch.season?.takeIf { it.number != 0 }?.let { season ->
                    context.getString(
                        R.string.tv_show_watch_season_episode,
                        season.number,
                        episodeToWatch.number
                    )
                } ?: context.getString(
                    R.string.tv_show_watch_episode,
                    episodeToWatch.number
                )
            } else ""
            visibility = when {
                episodeToWatch != null -> View.VISIBLE
                else -> View.GONE
            }
        }

        binding.pbTvShowWatchEpisodeLoading.apply {
            visibility = when {
                episodeToWatch != null -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.pbTvShowProgressEpisode.apply {
            val watchHistory = episodeToWatch?.watchHistory

            progress = when {
                watchHistory != null -> (watchHistory.lastPlaybackPositionMillis * 100 / watchHistory.durationMillis.toDouble()).toInt()
                else -> 0
            }
            visibility = when {
                watchHistory != null -> View.VISIBLE
                else -> View.GONE
            }
        }

        binding.btnTvShowTrailer.apply {
            val trailer = tvShow.trailer

            setOnClickListener {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(trailer)
                    )
                )
            }

            visibility = when {
                trailer != null -> View.VISIBLE
                else -> View.GONE
            }
        }

        binding.btnTvShowFavorite.apply {
            fun Boolean.drawable() = when (this) {
                true -> R.drawable.ic_favorite_enable
                false -> R.drawable.ic_favorite_disable
            }

            setOnClickListener {
                tvShow.isFavorite = !tvShow.isFavorite
                database.tvShowDao().update(tvShow)

                setImageDrawable(
                    ContextCompat.getDrawable(context, tvShow.isFavorite.drawable())
                )
            }

            setImageDrawable(
                ContextCompat.getDrawable(context, tvShow.isFavorite.drawable())
            )
        }
    }

    private fun displaySeasonsMobile(binding: ContentTvShowSeasonsMobileBinding) {
        binding.rvTvShowSeasons.apply {
            adapter = AppAdapter().apply {
                submitList(tvShow.seasons.onEach {
                    it.itemType = AppAdapter.Type.SEASON_MOBILE_ITEM
                })
            }
            if (itemDecorationCount == 0) {
                addItemDecoration(SpacingItemDecoration(20.dp(context)))
            }
        }
    }

    private fun displaySeasonsTv(binding: ContentTvShowSeasonsTvBinding) {
        binding.hgvTvShowSeasons.apply {
            setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
            adapter = AppAdapter().apply {
                submitList(tvShow.seasons.onEach {
                    it.itemType = AppAdapter.Type.SEASON_TV_ITEM
                })
            }
            setItemSpacing(80)
        }
    }

    private fun displayCastMobile(binding: ContentTvShowCastMobileBinding) {
        binding.rvTvShowCast.apply {
            adapter = AppAdapter().apply {
                submitList(tvShow.cast.onEach {
                    it.itemType = AppAdapter.Type.PEOPLE_MOBILE_ITEM
                })
            }
            if (itemDecorationCount == 0) {
                addItemDecoration(SpacingItemDecoration(20.dp(context)))
            }
        }
    }

    private fun displayCastTv(binding: ContentTvShowCastTvBinding) {
        binding.hgvTvShowCast.apply {
            setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
            adapter = AppAdapter().apply {
                submitList(tvShow.cast.onEach {
                    it.itemType = AppAdapter.Type.PEOPLE_TV_ITEM
                })
            }
            setItemSpacing(80)
        }
    }

    private fun displayRecommendationsMobile(binding: ContentTvShowRecommendationsMobileBinding) {
        binding.rvTvShowRecommendations.apply {
            adapter = AppAdapter().apply {
                submitList(tvShow.recommendations.onEach {
                    when (it) {
                        is Movie -> it.itemType = AppAdapter.Type.MOVIE_MOBILE_ITEM
                        is TvShow -> it.itemType = AppAdapter.Type.TV_SHOW_MOBILE_ITEM
                    }
                })
            }
            if (itemDecorationCount == 0) {
                addItemDecoration(SpacingItemDecoration(10.dp(context)))
            }
        }
    }

    private fun displayRecommendationsTv(binding: ContentTvShowRecommendationsTvBinding) {
        binding.hgvTvShowRecommendations.apply {
            setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
            adapter = AppAdapter().apply {
                submitList(tvShow.recommendations.onEach {
                    when (it) {
                        is Movie -> it.itemType = AppAdapter.Type.MOVIE_TV_ITEM
                        is TvShow -> it.itemType = AppAdapter.Type.TV_SHOW_TV_ITEM
                    }
                })
            }
            setItemSpacing(20)
        }
    }
}