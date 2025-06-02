package com.tanasi.streamflix.ui

import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import androidx.navigation.fragment.NavHostFragment
import com.bumptech.glide.Glide
import com.tanasi.streamflix.R
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.database.AppDatabase
import com.tanasi.streamflix.databinding.DialogShowOptionsTvBinding
import com.tanasi.streamflix.fragments.home.HomeTvFragment
import com.tanasi.streamflix.fragments.home.HomeTvFragmentDirections
import com.tanasi.streamflix.models.Episode
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.utils.format
import com.tanasi.streamflix.utils.getCurrentFragment
import com.tanasi.streamflix.utils.toActivity
import java.util.Calendar

class ShowOptionsTvDialog(
    context: Context,
    show: AppAdapter.Item,
) : Dialog(context) {

    private val binding = DialogShowOptionsTvBinding.inflate(LayoutInflater.from(context))

    private val database = AppDatabase.getInstance(context)

    init {
        setContentView(binding.root)

        binding.btnOptionCancel.setOnClickListener {
            hide()
        }

        when (show) {
            is Episode -> displayEpisode(show)
            is Movie -> displayMovie(show)
            is TvShow -> displayTvShow(show)
        }


        window?.attributes = window?.attributes?.also { param ->
            param.gravity = Gravity.END
        }
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.35).toInt(),
            context.resources.displayMetrics.heightPixels
        )
    }


    private fun displayEpisode(episode: Episode) {
        Glide.with(context)
            .load(episode.poster ?: episode.tvShow?.poster)
            .fallback(R.drawable.glide_fallback_cover)
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
                    is HomeTvFragment -> episode.tvShow?.let { tvShow ->
                        NavHostFragment.findNavController(fragment).navigate(
                            HomeTvFragmentDirections.actionHomeToTvShow(
                                id = tvShow.id
                            )
                        )
                    }
                }
                hide()
            }

            visibility = when (context.toActivity()?.getCurrentFragment()) {
                is HomeTvFragment -> View.VISIBLE
                else -> View.GONE
            }

            requestFocus()
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
4

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
            .fallback(R.drawable.glide_fallback_cover)
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

            requestFocus()
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
            .fallback(R.drawable.glide_fallback_cover)
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

            requestFocus()
        }

        binding.btnOptionShowWatched.visibility = View.GONE

        binding.btnOptionProgramClear.visibility = View.GONE
    }
}