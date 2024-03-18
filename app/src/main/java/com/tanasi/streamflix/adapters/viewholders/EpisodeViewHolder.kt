package com.tanasi.streamflix.adapters.viewholders

import android.view.View
import android.view.animation.AnimationUtils
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.tanasi.streamflix.R
import com.tanasi.streamflix.database.AppDatabase
import com.tanasi.streamflix.databinding.ItemEpisodeBinding
import com.tanasi.streamflix.databinding.ItemEpisodeContinueWatchingBinding
import com.tanasi.streamflix.databinding.ItemEpisodeContinueWatchingMobileBinding
import com.tanasi.streamflix.databinding.ItemEpisodeMobileBinding
import com.tanasi.streamflix.fragments.home.HomeFragment
import com.tanasi.streamflix.fragments.home.HomeFragmentDirections
import com.tanasi.streamflix.fragments.home.HomeMobileFragmentDirections
import com.tanasi.streamflix.fragments.player.PlayerFragment
import com.tanasi.streamflix.fragments.season.SeasonFragmentDirections
import com.tanasi.streamflix.fragments.season.SeasonMobileFragmentDirections
import com.tanasi.streamflix.models.Episode
import com.tanasi.streamflix.ui.ShowOptionsDialog
import com.tanasi.streamflix.ui.ShowOptionsMobileDialog
import com.tanasi.streamflix.utils.getCurrentFragment
import com.tanasi.streamflix.utils.toActivity

class EpisodeViewHolder(
    private val _binding: ViewBinding
) : RecyclerView.ViewHolder(
    _binding.root
) {

    private val context = itemView.context
    private val database = AppDatabase.getInstance(context)
    private lateinit var episode: Episode

    fun bind(episode: Episode) {
        this.episode = episode

        when (_binding) {
            is ItemEpisodeMobileBinding -> displayMobileItem(_binding)
            is ItemEpisodeBinding -> displayItem(_binding)
            is ItemEpisodeContinueWatchingMobileBinding -> displayContinueWatchingMobileItem(_binding)
            is ItemEpisodeContinueWatchingBinding -> displayContinueWatchingItem(_binding)
        }
    }


    private fun displayMobileItem(binding: ItemEpisodeMobileBinding) {
        database.episodeDao().getById(episode.id)?.let { episodeDb ->
            episode.merge(episodeDb)
        }

        binding.root.apply {
            setOnClickListener {
                findNavController().navigate(
                    SeasonMobileFragmentDirections.actionSeasonToPlayer(
                        id = episode.id,
                        title = episode.tvShow?.title ?: "",
                        subtitle = episode.season?.takeIf { it.number != 0 }?.let { season ->
                            context.getString(
                                R.string.player_subtitle_tv_show,
                                season.number,
                                episode.number,
                                episode.title
                            )
                        } ?: context.getString(
                            R.string.player_subtitle_tv_show_episode_only,
                            episode.number,
                            episode.title
                        ),
                        videoType = PlayerFragment.VideoType.Episode(
                            id = episode.id,
                            number = episode.number,
                            title = episode.title,
                            poster = episode.poster,
                            tvShow = PlayerFragment.VideoType.Episode.TvShow(
                                id = episode.tvShow?.id ?: "",
                                title = episode.tvShow?.title ?: "",
                                poster = episode.tvShow?.poster,
                                banner = episode.tvShow?.banner,
                            ),
                            season = PlayerFragment.VideoType.Episode.Season(
                                number = episode.season?.number ?: 0,
                                title = episode.season?.title ?: "",
                            ),
                        ),
                    )
                )
            }
            setOnLongClickListener {
                ShowOptionsMobileDialog(context, episode)
                    .show()
                true
            }
        }

        binding.ivEpisodePoster.apply {
            clipToOutline = true
            Glide.with(context)
                .load(episode.poster)
                .centerCrop()
                .into(this)
        }

        binding.pbEpisodeProgress.apply {
            val watchHistory = episode.watchHistory

            progress = when {
                watchHistory != null -> (watchHistory.lastPlaybackPositionMillis * 100 / watchHistory.durationMillis.toDouble()).toInt()
                episode.isWatched -> 100
                else -> 0
            }
            visibility = when {
                watchHistory != null -> View.VISIBLE
                episode.isWatched -> View.VISIBLE
                else -> View.GONE
            }
        }

        binding.tvEpisodeInfo.text = context.getString(
            R.string.seasons_episode_number,
            episode.number
        )

        binding.tvEpisodeTitle.text = episode.title
    }

    private fun displayItem(binding: ItemEpisodeBinding) {
        database.episodeDao().getById(episode.id)?.let { episodeDb ->
            episode.merge(episodeDb)
        }

        binding.root.apply {
            setOnClickListener {
                findNavController().navigate(
                    SeasonFragmentDirections.actionSeasonToPlayer(
                        id = episode.id,
                        title = episode.tvShow?.title ?: "",
                        subtitle = episode.season?.takeIf { it.number != 0 }?.let { season ->
                            context.getString(
                                R.string.player_subtitle_tv_show,
                                season.number,
                                episode.number,
                                episode.title
                            )
                        } ?: context.getString(
                            R.string.player_subtitle_tv_show_episode_only,
                            episode.number,
                            episode.title
                        ),
                        videoType = PlayerFragment.VideoType.Episode(
                            id = episode.id,
                            number = episode.number,
                            title = episode.title,
                            poster = episode.poster,
                            tvShow = PlayerFragment.VideoType.Episode.TvShow(
                                id = episode.tvShow?.id ?: "",
                                title = episode.tvShow?.title ?: "",
                                poster = episode.tvShow?.poster,
                                banner = episode.tvShow?.banner,
                            ),
                            season = PlayerFragment.VideoType.Episode.Season(
                                number = episode.season?.number ?: 0,
                                title = episode.season?.title ?: "",
                            ),
                        ),
                    )
                )
            }
            setOnLongClickListener {
                ShowOptionsDialog(context, episode)
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

        binding.ivEpisodePoster.apply {
            clipToOutline = true
            Glide.with(context)
                .load(episode.poster)
                .centerCrop()
                .into(this)
        }

        binding.pbEpisodeProgress.apply {
            val watchHistory = episode.watchHistory

            progress = when {
                watchHistory != null -> (watchHistory.lastPlaybackPositionMillis * 100 / watchHistory.durationMillis.toDouble()).toInt()
                episode.isWatched -> 100
                else -> 0
            }
            visibility = when {
                watchHistory != null -> View.VISIBLE
                episode.isWatched -> View.VISIBLE
                else -> View.GONE
            }
        }

        binding.tvEpisodeInfo.text = context.getString(
            R.string.seasons_episode_number,
            episode.number
        )

        binding.tvEpisodeTitle.text = episode.title
    }

    private fun displayContinueWatchingMobileItem(binding: ItemEpisodeContinueWatchingMobileBinding) {
        database.episodeDao().getById(episode.id)?.let { episodeDb ->
            episode.merge(episodeDb)
        }

        binding.root.apply {
            setOnClickListener {
                findNavController().navigate(
                    HomeMobileFragmentDirections.actionHomeToPlayer(
                        id = episode.id,
                        title = episode.tvShow?.title ?: "",
                        subtitle = episode.season?.takeIf { it.number != 0 }?.let { season ->
                            context.getString(
                                R.string.player_subtitle_tv_show,
                                season.number,
                                episode.number,
                                episode.title
                            )
                        } ?: context.getString(
                            R.string.player_subtitle_tv_show_episode_only,
                            episode.number,
                            episode.title
                        ),
                        videoType = PlayerFragment.VideoType.Episode(
                            id = episode.id,
                            number = episode.number,
                            title = episode.title,
                            poster = episode.poster,
                            tvShow = PlayerFragment.VideoType.Episode.TvShow(
                                id = episode.tvShow?.id ?: "",
                                title = episode.tvShow?.title ?: "",
                                poster = episode.tvShow?.poster,
                                banner = episode.tvShow?.banner,
                            ),
                            season = PlayerFragment.VideoType.Episode.Season(
                                number = episode.season?.number ?: 0,
                                title = episode.season?.title ?: "",
                            ),
                        ),
                    )
                )
            }
            setOnLongClickListener {
                ShowOptionsMobileDialog(context, episode)
                    .show()
                true
            }
        }

        binding.ivEpisodeTvShowPoster.apply {
            clipToOutline = true
            Glide.with(context)
                .load(episode.tvShow?.poster ?: episode.tvShow?.banner ?: episode.poster)
                .centerCrop()
                .into(this)
        }

        binding.pbEpisodeProgress.apply {
            val watchHistory = episode.watchHistory

            progress = when {
                watchHistory != null -> (watchHistory.lastPlaybackPositionMillis * 100 / watchHistory.durationMillis.toDouble()).toInt()
                else -> 0
            }
            visibility = when {
                watchHistory != null -> View.VISIBLE
                else -> View.GONE
            }
        }

        binding.tvEpisodeTvShowTitle.text = episode.tvShow?.title ?: ""

        binding.tvEpisodeInfo.text = episode.season?.takeIf { it.number != 0 }?.let { season ->
            context.getString(
                R.string.episode_item_info,
                season.number,
                episode.number,
                episode.title
            )
        } ?: context.getString(
            R.string.episode_item_info_episode_only,
            episode.number,
            episode.title
        )
    }

    private fun displayContinueWatchingItem(binding: ItemEpisodeContinueWatchingBinding) {
        database.episodeDao().getById(episode.id)?.let { episodeDb ->
            episode.merge(episodeDb)
        }

        binding.root.apply {
            setOnClickListener {
                findNavController().navigate(
                    HomeFragmentDirections.actionHomeToPlayer(
                        id = episode.id,
                        title = episode.tvShow?.title ?: "",
                        subtitle = episode.season?.takeIf { it.number != 0 }?.let { season ->
                            context.getString(
                                R.string.player_subtitle_tv_show,
                                season.number,
                                episode.number,
                                episode.title
                            )
                        } ?: context.getString(
                            R.string.player_subtitle_tv_show_episode_only,
                            episode.number,
                            episode.title
                        ),
                        videoType = PlayerFragment.VideoType.Episode(
                            id = episode.id,
                            number = episode.number,
                            title = episode.title,
                            poster = episode.poster,
                            tvShow = PlayerFragment.VideoType.Episode.TvShow(
                                id = episode.tvShow?.id ?: "",
                                title = episode.tvShow?.title ?: "",
                                poster = episode.tvShow?.poster,
                                banner = episode.tvShow?.banner,
                            ),
                            season = PlayerFragment.VideoType.Episode.Season(
                                number = episode.season?.number ?: 0,
                                title = episode.season?.title ?: "",
                            ),
                        ),
                    )
                )
            }
            setOnLongClickListener {
                ShowOptionsDialog(context, episode)
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
                        is HomeFragment -> fragment.updateBackground(episode.tvShow?.banner)
                    }
                }
            }
        }

        binding.ivEpisodeTvShowPoster.apply {
            clipToOutline = true
            Glide.with(context)
                .load(episode.tvShow?.poster ?: episode.tvShow?.banner ?: episode.poster)
                .centerCrop()
                .into(this)
        }

        binding.pbEpisodeProgress.apply {
            val watchHistory = episode.watchHistory

            progress = when {
                watchHistory != null -> (watchHistory.lastPlaybackPositionMillis * 100 / watchHistory.durationMillis.toDouble()).toInt()
                episode.isWatched -> 100
                else -> 0
            }
            visibility = when {
                watchHistory != null -> View.VISIBLE
                episode.isWatched -> View.VISIBLE
                else -> View.GONE
            }
        }

        binding.tvEpisodeTvShowTitle.text = episode.tvShow?.title ?: ""

        binding.tvEpisodeInfo.text = episode.season?.takeIf { it.number != 0 }?.let { season ->
            context.getString(
                R.string.episode_item_info,
                season.number,
                episode.number,
                episode.title
            )
        } ?: context.getString(
            R.string.episode_item_info_episode_only,
            episode.number,
            episode.title
        )
    }
}