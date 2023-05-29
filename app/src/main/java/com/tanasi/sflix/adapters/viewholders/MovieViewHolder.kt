package com.tanasi.sflix.adapters.viewholders

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.tvprovider.media.tv.TvContractCompat
import androidx.tvprovider.media.tv.WatchNextProgram
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.tanasi.sflix.R
import com.tanasi.sflix.adapters.SflixAdapter
import com.tanasi.sflix.databinding.*
import com.tanasi.sflix.fragments.genre.GenreFragment
import com.tanasi.sflix.fragments.genre.GenreFragmentDirections
import com.tanasi.sflix.fragments.home.HomeFragment
import com.tanasi.sflix.fragments.home.HomeFragmentDirections
import com.tanasi.sflix.fragments.movie.MovieFragment
import com.tanasi.sflix.fragments.movie.MovieFragmentDirections
import com.tanasi.sflix.fragments.movies.MoviesFragment
import com.tanasi.sflix.fragments.movies.MoviesFragmentDirections
import com.tanasi.sflix.fragments.people.PeopleFragment
import com.tanasi.sflix.fragments.people.PeopleFragmentDirections
import com.tanasi.sflix.fragments.player.PlayerFragment
import com.tanasi.sflix.fragments.search.SearchFragment
import com.tanasi.sflix.fragments.search.SearchFragmentDirections
import com.tanasi.sflix.fragments.tv_show.TvShowFragment
import com.tanasi.sflix.fragments.tv_show.TvShowFragmentDirections
import com.tanasi.sflix.models.Movie
import com.tanasi.sflix.models.TvShow
import com.tanasi.sflix.utils.*

@SuppressLint("RestrictedApi")
class MovieViewHolder(
    private val _binding: ViewBinding
) : RecyclerView.ViewHolder(
    _binding.root
) {

    private val context = itemView.context
    private lateinit var movie: Movie

    fun bind(movie: Movie) {
        this.movie = movie

        when (_binding) {
            is ItemMovieBinding -> displayItem(_binding)
            is ItemMovieGridBinding -> displayGridItem(_binding)
            is ItemMovieContinueWatchingBinding -> displayItemContinueWatching(_binding)

            is ContentMovieBinding -> displayMovie(_binding)
            is ContentMovieCastsBinding -> displayCasts(_binding)
            is ContentMovieRecommendationsBinding -> displayRecommendations(_binding)
        }
    }


    private fun displayItem(binding: ItemMovieBinding) {
        binding.root.apply {
            setOnClickListener {
                when (context.toActivity()?.getCurrentFragment()) {
                    is HomeFragment -> findNavController().navigate(
                        HomeFragmentDirections.actionHomeToMovie(
                            id = movie.id
                        )
                    )
                    is MovieFragment -> findNavController().navigate(
                        MovieFragmentDirections.actionMovieToMovie(
                            id = movie.id
                        )
                    )
                    is PeopleFragment -> findNavController().navigate(
                        PeopleFragmentDirections.actionPeopleToMovie(
                            id = movie.id
                        )
                    )
                    is TvShowFragment -> findNavController().navigate(
                        TvShowFragmentDirections.actionTvShowToMovie(
                            id = movie.id
                        )
                    )
                }
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
                        is HomeFragment -> fragment.updateBackground(movie.banner)
                    }
                }
            }
        }

        Glide.with(context)
            .load(movie.poster)
            .centerCrop()
            .into(binding.ivMoviePoster)

        binding.tvMovieQuality.apply {
            text = movie.quality ?: ""
            visibility = when {
                text.isNullOrEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.tvMovieReleasedYear.text = movie.released?.format("yyyy")
            ?: context.getString(R.string.movie_item_type)

        binding.tvMovieTitle.text = movie.title
    }

    private fun displayGridItem(binding: ItemMovieGridBinding) {
        binding.root.apply {
            setOnClickListener {
                when (context.toActivity()?.getCurrentFragment()) {
                    is GenreFragment -> findNavController().navigate(
                        GenreFragmentDirections.actionGenreToMovie(
                            id = movie.id
                        )
                    )
                    is MoviesFragment -> findNavController().navigate(
                        MoviesFragmentDirections.actionMoviesToMovie(
                            id = movie.id
                        )
                    )
                    is SearchFragment -> findNavController().navigate(
                        SearchFragmentDirections.actionSearchToMovie(
                            id = movie.id
                        )
                    )
                }

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
            .load(movie.poster)
            .centerCrop()
            .into(binding.ivMoviePoster)

        binding.tvMovieQuality.apply {
            text = movie.quality ?: ""
            visibility = when {
                text.isNullOrEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.tvMovieReleasedYear.text = movie.released?.format("yyyy")
            ?: context.getString(R.string.movie_item_type)

        binding.tvMovieTitle.text = movie.title
    }

    private fun displayItemContinueWatching(binding: ItemMovieContinueWatchingBinding) {
        binding.root.apply {
            setOnClickListener {
                findNavController().navigate(
                    HomeFragmentDirections.actionHomeToPlayer(
                        id = movie.id,
                        title = movie.title,
                        subtitle = movie.released?.format("yyyy") ?: "",
                        videoType = PlayerFragment.VideoType.Movie(
                            id = movie.id,
                            title = movie.title,
                            releaseDate = movie.released?.format("yyyy-MM-dd") ?: "",
                            poster = movie.poster ?: "",
                        ),
                    )
                )
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
                        is HomeFragment -> fragment.updateBackground(movie.banner)
                    }
                }
            }
        }

        Glide.with(context)
            .load(movie.poster)
            .centerCrop()
            .into(binding.ivMoviePoster)

        binding.pbMovieProgress.apply {
            val program = context.contentResolver.query(
                TvContractCompat.WatchNextPrograms.CONTENT_URI,
                WatchNextProgram.PROJECTION,
                null,
                null,
                null,
            )?.map { WatchNextProgram.fromCursor(it) }
                ?.find { it.contentId == movie.id && it.internalProviderId == UserPreferences.currentProvider.name }

            progress = when {
                program != null -> (program.lastPlaybackPositionMillis * 100 / program.durationMillis.toDouble()).toInt()
                else -> 0
            }
            visibility = when {
                program != null -> View.VISIBLE
                else -> View.GONE
            }
        }

        binding.tvMovieReleasedYear.text = movie.released?.format("yyyy") ?: ""

        binding.tvMovieTitle.text = movie.title
    }


    private fun displayMovie(binding: ContentMovieBinding) {
        binding.ivMoviePoster.run {
            Glide.with(context)
                .load(movie.poster)
                .into(this)
            visibility = when {
                movie.poster.isNullOrEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.tvMovieTitle.text = movie.title

        binding.tvMovieRating.text = movie.rating?.let { String.format("%.1f", it) } ?: "N/A"

        binding.tvMovieQuality.apply {
            text = movie.quality
            visibility = when {
                text.isNullOrEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.tvMovieReleased.apply {
            text = movie.released?.format("yyyy")
            visibility = when {
                text.isNullOrEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.tvMovieRuntime.apply {
            text = movie.runtime?.let {
                val hours = it / 60
                val minutes = it % 60
                when {
                    hours > 0 -> context.getString(
                        R.string.movie_runtime_hours_minutes,
                        hours,
                        minutes
                    )
                    else -> context.getString(R.string.movie_runtime_minutes, minutes)
                }
            }
            visibility = when {
                text.isNullOrEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.tvMovieOverview.text = movie.overview

        binding.btnMovieWatchNow.apply {
            setOnClickListener {
                findNavController().navigate(
                    MovieFragmentDirections.actionMovieToPlayer(
                        id = movie.id,
                        title = movie.title,
                        subtitle = movie.released?.format("yyyy") ?: "",
                        videoType = PlayerFragment.VideoType.Movie(
                            id = movie.id,
                            title = movie.title,
                            releaseDate = movie.released?.format("yyyy-MM-dd") ?: "",
                            poster = movie.poster ?: movie.banner ?: "",
                        ),
                    )
                )
            }
        }

        binding.pbMovieProgress.apply {
            val program = context.contentResolver.query(
                TvContractCompat.WatchNextPrograms.CONTENT_URI,
                WatchNextProgram.PROJECTION,
                null,
                null,
                null,
            )?.map { WatchNextProgram.fromCursor(it) }
                ?.find { it.contentId == movie.id && it.internalProviderId == UserPreferences.currentProvider.name }

            progress = when {
                program != null -> (program.lastPlaybackPositionMillis * 100 / program.durationMillis.toDouble()).toInt()
                else -> 0
            }
            visibility = when {
                program != null -> View.VISIBLE
                else -> View.GONE
            }
        }

        binding.btnMovieTrailer.setOnClickListener {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.youtube.com/watch?v=${movie.youtubeTrailerId}")
                )
            )
        }
    }

    private fun displayCasts(binding: ContentMovieCastsBinding) {
        binding.hgvMovieCasts.apply {
            setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
            adapter = SflixAdapter().apply {
                items.addAll(movie.cast)
            }
            setItemSpacing(80)
        }
    }

    private fun displayRecommendations(binding: ContentMovieRecommendationsBinding) {
        binding.hgvMovieRecommendations.apply {
            setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
            adapter = SflixAdapter().apply {
                items.addAll(movie.recommendations.onEach {
                    when (it) {
                        is Movie -> it.itemType = SflixAdapter.Type.MOVIE_ITEM
                        is TvShow -> it.itemType = SflixAdapter.Type.TV_SHOW_ITEM
                    }
                })
            }
            setItemSpacing(20)
        }
    }
}