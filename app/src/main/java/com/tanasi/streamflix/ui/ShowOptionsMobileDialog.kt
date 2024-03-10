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
import com.tanasi.streamflix.fragments.season.SeasonMobileFragment
import com.tanasi.streamflix.models.Episode
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.utils.WatchNextUtils
import com.tanasi.streamflix.utils.format
import com.tanasi.streamflix.utils.getCurrentFragment
import com.tanasi.streamflix.utils.toActivity

class ShowOptionsMobileDialog(context: Context) : BottomSheetDialog(context) {

    private val binding = DialogShowOptionsMobileBinding.inflate(LayoutInflater.from(context))

    private val database = AppDatabase.getInstance(context)

    var show: AppAdapter.Item? = null
        set(value) {
            when (value) {
                is Episode -> displayEpisode(value)
                is Movie -> displayMovie(value)
                is TvShow -> displayTvShow(value)
            }
            field = value
        }

    init {
        setContentView(binding.root)

        findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.setBackgroundColor(Color.TRANSPARENT)

        binding.btnOptionCancel.setOnClickListener {
            hide()
        }
    }


    private fun displayEpisode(episode: Episode) {
        val saved = database.episodeDao().getById(episode.id)?.also {
            episode.isWatched = it.isWatched
        }
        val program = WatchNextUtils.getProgram(context, episode.id)

        Glide.with(context)
            .load(episode.poster ?: episode.tvShow?.poster)
            .fitCenter()
            .into(binding.ivOptionsShowPoster)

        binding.tvOptionsShowTitle.text = episode.title

        binding.tvShowSubtitle.text = episode.season?.number?.let { seasonNumber ->
            context.getString(
                R.string.episode_item_info,
                seasonNumber,
                episode.number,
                episode.title,
            )
        } ?: context.getString(
            R.string.episode_item_info_episode_only,
            episode.number,
            episode.title,
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
                saved?.let {
                    database.episodeDao().updateWatched(
                        id = episode.id,
                        isWatched = !episode.isWatched
                    )
                    episode.isWatched = !episode.isWatched
                } ?: let { _ ->
                    episode.isWatched = !episode.isWatched
                    database.episodeDao().insert(episode)
                }
                if (episode.isWatched) {
                    program?.let { WatchNextUtils.deleteProgramById(context, program.id) }
                }

                when (val fragment = context.toActivity()?.getCurrentFragment()) {
                    is HomeMobileFragment -> fragment.refresh()
                    is SeasonMobileFragment -> fragment.refresh(episode)
                }
                hide()
            }

            text = when {
                episode.isWatched -> context.getString(R.string.option_show_unwatched)
                else -> context.getString(R.string.option_show_watched)
            }
            visibility = View.VISIBLE
        }

        binding.btnOptionProgramClear.apply {
            setOnClickListener {
                if (program == null) return@setOnClickListener

                WatchNextUtils.deleteProgramById(context, program.id)
                when (val fragment = context.toActivity()?.getCurrentFragment()) {
                    is HomeMobileFragment -> fragment.refresh()
                    is SeasonMobileFragment -> fragment.refresh(episode)
                }
                hide()
            }

            visibility = when {
                program != null -> View.VISIBLE
                else -> View.GONE
            }
        }
    }

    private fun displayMovie(movie: Movie) {
        val saved = database.movieDao().getById(movie.id)?.also {
            movie.isFavorite = it.isFavorite
            movie.isWatched = it.isWatched
        }
        val program = WatchNextUtils.getProgram(context, movie.id)

        Glide.with(context)
            .load(movie.poster)
            .fitCenter()
            .into(binding.ivOptionsShowPoster)

        binding.tvOptionsShowTitle.text = movie.title

        binding.tvShowSubtitle.text = movie.released?.format("yyyy")


        binding.btnOptionEpisodeOpenTvShow.visibility = View.GONE

        binding.btnOptionShowFavorite.apply {
            setOnClickListener {
                saved?.let {
                    database.movieDao().updateFavorite(
                        id = movie.id,
                        isFavorite = !movie.isFavorite
                    )
                    movie.isFavorite = !movie.isFavorite
                } ?: let { _ ->
                    movie.isFavorite = !movie.isFavorite
                    database.movieDao().insert(movie)
                }

                when (val fragment = context.toActivity()?.getCurrentFragment()) {
                    is HomeMobileFragment -> fragment.refresh()
                }
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
                saved?.let {
                    database.movieDao().updateWatched(
                        id = movie.id,
                        isWatched = !movie.isWatched
                    )
                    movie.isWatched = !movie.isWatched
                } ?: let { _ ->
                    movie.isWatched = !movie.isWatched
                    database.movieDao().insert(movie)
                }
                if (movie.isWatched) {
                    program?.let { WatchNextUtils.deleteProgramById(context, program.id) }
                }

                when (val fragment = context.toActivity()?.getCurrentFragment()) {
                    is HomeMobileFragment -> fragment.refresh()
                }
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
                if (program == null) return@setOnClickListener

                WatchNextUtils.deleteProgramById(context, program.id)
                when (val fragment = context.toActivity()?.getCurrentFragment()) {
                    is HomeMobileFragment -> fragment.refresh()
                }
                hide()
            }

            visibility = when {
                program != null -> View.VISIBLE
                else -> View.GONE
            }
        }
    }

    private fun displayTvShow(tvShow: TvShow) {
        val saved = database.tvShowDao().getById(tvShow.id)?.also {
            tvShow.isFavorite = it.isFavorite
        }

        Glide.with(context)
            .load(tvShow.poster)
            .fitCenter()
            .into(binding.ivOptionsShowPoster)

        binding.tvOptionsShowTitle.text = tvShow.title

        binding.tvShowSubtitle.text = tvShow.released?.format("yyyy")


        binding.btnOptionEpisodeOpenTvShow.visibility = View.GONE

        binding.btnOptionShowFavorite.apply {
            setOnClickListener {
                saved?.let {
                    database.tvShowDao().updateFavorite(
                        id = tvShow.id,
                        isFavorite = !tvShow.isFavorite
                    )
                    tvShow.isFavorite = !tvShow.isFavorite
                } ?: let {
                    tvShow.isFavorite = !tvShow.isFavorite
                    database.tvShowDao().insert(tvShow)
                }

                when (val fragment = context.toActivity()?.getCurrentFragment()) {
                    is HomeMobileFragment -> fragment.refresh()
                }
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