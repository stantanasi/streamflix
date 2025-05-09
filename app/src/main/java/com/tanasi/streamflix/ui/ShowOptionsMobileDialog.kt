package com.tanasi.streamflix.ui

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import androidx.navigation.fragment.NavHostFragment
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tanasi.streamflix.R
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.database.AppDatabase
import com.tanasi.streamflix.databinding.DialogShowOptionsMobileBinding
import com.tanasi.streamflix.fragments.home.HomeMobileFragment
import com.tanasi.streamflix.fragments.home.HomeMobileFragmentDirections
import com.tanasi.streamflix.models.Episode
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.utils.format
import com.tanasi.streamflix.utils.getCurrentFragment
import com.tanasi.streamflix.utils.toActivity
import java.util.Calendar

class ShowOptionsMobileDialog(
    context: Context,
    show: AppAdapter.Item,
) : BottomSheetDialog(context) {

    private val binding = DialogShowOptionsMobileBinding.inflate(LayoutInflater.from(context))

    private val database = AppDatabase.getInstance(context)

    init {
        setContentView(binding.root)

        findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.setBackgroundColor(Color.TRANSPARENT)

        when (show) {
            is Episode -> displayEpisode(show)
            is Movie -> displayMovie(show)
            is TvShow -> displayTvShow(show)
        }

        binding.btnOptionCancel.setOnClickListener {
            hide()
        }
    }


    private fun displayEpisode(episode: Episode) {
        Glide.with(context)
            .load(episode.poster ?: episode.tvShow?.poster)
            .fitCenter()
            .into(binding.ivOptionsShowPoster)

        binding.tvOptionsShowTitle.text = episode.tvShow?.title ?: ""

        binding.tvShowSubtitle.text = episode.season?.takeIf { it.number != 0 }?.let { season ->
            context.getString(
                R.string.episode_item_info,
                season.number,
                episode.number,
                episode.title ?: context.getString(
                    R.string.episode_number,
                    episode.number
                )
            )
        } ?: context.getString(
            R.string.episode_item_info_episode_only,
            episode.number,
            episode.title ?: context.getString(
                R.string.episode_number,
                episode.number
            )
        )


        binding.btnOptionEpisodeOpenTvShow.apply {
            setOnClickListener {
                when (val fragment = context.toActivity()?.getCurrentFragment()) {
                    is HomeMobileFragment -> episode.tvShow?.let { tvShow ->
                        NavHostFragment.findNavController(fragment).navigate(
                            HomeMobileFragmentDirections.actionHomeToTvShow(
                                id = tvShow.id
                            )
                        )
                    }
                }
                hide()
            }

            visibility = when (context.toActivity()?.getCurrentFragment()) {
                is HomeMobileFragment -> View.VISIBLE
                else -> View.GONE
            }
        }

        binding.btnOptionShowFavorite.visibility = View.GONE

        binding.btnOptionShowWatched.apply {
            setOnClickListener {
                database.episodeDao().save(episode.copy().apply {
                    merge(episode)
                    isWatched = !isWatched
                    if (isWatched) {
                        watchedDate = Calendar.getInstance()
                        watchHistory = null
                    } else {
                        watchedDate = null
                    }
                })

                hide()
            }

            text = when {
                episode.isWatched -> context.getString(R.string.option_show_unwatched)
                else -> context.getString(R.string.option_show_watched)
            }
            visibility = View.VISIBLE
        }
        binding.btnOptionEpisodeMarkAllPreviousWatched.apply {
            setOnClickListener {
                val episodeDao = database.episodeDao()
                val episodeNumber = episode.number
                val tvShowId = episode.tvShow?.id ?: return@setOnClickListener
                val allEpisodes = episodeDao.getEpisodesByTvShowId(tvShowId)

                val targetState = !episode.isWatched // If current is watched, we unwatch; else, we mark watched
                val now = Calendar.getInstance()

                for (ep in allEpisodes) {
                    if (ep.number <= episodeNumber && ep.isWatched != targetState) {
                        episodeDao.save(ep.copy().apply {
                            merge(ep)
                            isWatched = targetState
                            watchedDate = if (targetState) now else null
                            watchHistory = if (targetState) null else watchHistory
                        })
                    }
                }

                hide()
            }

            text = when {
                episode.isWatched -> context.getString(R.string.option_show_mark_all_previous_unwatched)
                else -> context.getString(R.string.option_show_mark_all_previous_watched)
            }
            visibility = View.VISIBLE
        }

        binding.btnOptionProgramClear.apply {
            setOnClickListener {
                database.episodeDao().save(episode.copy().apply {
                    merge(episode)
                    watchHistory = null
                })
                episode.tvShow?.let { tvShow ->
                    database.tvShowDao().save(tvShow.copy().apply {
                        merge(tvShow)
                        isWatching = false
                    })
                }

                hide()
            }

            visibility = when {
                episode.watchHistory != null -> View.VISIBLE
                episode.tvShow?.isWatching ?: false -> View.VISIBLE
                else -> View.GONE
            }
        }
    }

    private fun displayMovie(movie: Movie) {
        Glide.with(context)
            .load(movie.poster)
            .fitCenter()
            .into(binding.ivOptionsShowPoster)

        binding.tvOptionsShowTitle.text = movie.title

        binding.tvShowSubtitle.text = movie.released?.format("yyyy")


        binding.btnOptionEpisodeOpenTvShow.visibility = View.GONE

        binding.btnOptionShowFavorite.apply {
            setOnClickListener {
                database.movieDao().save(movie.copy().apply {
                    merge(movie)
                    isFavorite = !isFavorite
                })

                hide()
            }

            text = when {
                movie.isFavorite -> context.getString(R.string.option_show_unfavorite)
                else -> context.getString(R.string.option_show_favorite)
            }
            visibility = View.VISIBLE
        }

        binding.btnOptionShowWatched.apply {
            setOnClickListener {
                database.movieDao().save(movie.copy().apply {
                    merge(movie)
                    isWatched = !isWatched
                    if (isWatched) {
                        watchedDate = Calendar.getInstance()
                        watchHistory = null
                    } else {
                        watchedDate = null
                    }
                })

                hide()
            }

            text = when {
                movie.isWatched -> context.getString(R.string.option_show_unwatched)
                else -> context.getString(R.string.option_show_watched)
            }
            visibility = View.VISIBLE
        }

        binding.btnOptionProgramClear.apply {
            setOnClickListener {
                database.movieDao().save(movie.copy().apply {
                    merge(movie)
                    watchHistory = null
                })

                hide()
            }

            visibility = when {
                movie.watchHistory != null -> View.VISIBLE
                else -> View.GONE
            }
        }
    }

    private fun displayTvShow(tvShow: TvShow) {
        Glide.with(context)
            .load(tvShow.poster)
            .fitCenter()
            .into(binding.ivOptionsShowPoster)

        binding.tvOptionsShowTitle.text = tvShow.title

        binding.tvShowSubtitle.text = tvShow.released?.format("yyyy")


        binding.btnOptionEpisodeOpenTvShow.visibility = View.GONE

        binding.btnOptionShowFavorite.apply {
            setOnClickListener {
                database.tvShowDao().save(tvShow.copy().apply {
                    merge(tvShow)
                    isFavorite = !isFavorite
                })

                hide()
            }

            text = when {
                tvShow.isFavorite -> context.getString(R.string.option_show_unfavorite)
                else -> context.getString(R.string.option_show_favorite)
            }
            visibility = View.VISIBLE
        }

        binding.btnOptionShowWatched.visibility = View.GONE

        binding.btnOptionProgramClear.visibility = View.GONE
    }
}