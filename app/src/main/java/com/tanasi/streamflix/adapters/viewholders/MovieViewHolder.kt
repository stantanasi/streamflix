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
import com.tanasi.streamflix.R
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.database.AppDatabase
import com.tanasi.streamflix.databinding.ContentMovieBinding
import com.tanasi.streamflix.databinding.ContentMovieCastsBinding
import com.tanasi.streamflix.databinding.ContentMovieCastsMobileBinding
import com.tanasi.streamflix.databinding.ContentMovieMobileBinding
import com.tanasi.streamflix.databinding.ContentMovieRecommendationsBinding
import com.tanasi.streamflix.databinding.ContentMovieRecommendationsMobileBinding
import com.tanasi.streamflix.databinding.ItemMovieBinding
import com.tanasi.streamflix.databinding.ItemMovieGridBinding
import com.tanasi.streamflix.databinding.ItemMovieGridMobileBinding
import com.tanasi.streamflix.databinding.ItemMovieMobileBinding
import com.tanasi.streamflix.fragments.genre.GenreFragment
import com.tanasi.streamflix.fragments.genre.GenreFragmentDirections
import com.tanasi.streamflix.fragments.genre.GenreMobileFragment
import com.tanasi.streamflix.fragments.genre.GenreMobileFragmentDirections
import com.tanasi.streamflix.fragments.home.HomeFragment
import com.tanasi.streamflix.fragments.home.HomeFragmentDirections
import com.tanasi.streamflix.fragments.home.HomeMobileFragment
import com.tanasi.streamflix.fragments.home.HomeMobileFragmentDirections
import com.tanasi.streamflix.fragments.movie.MovieFragment
import com.tanasi.streamflix.fragments.movie.MovieFragmentDirections
import com.tanasi.streamflix.fragments.movie.MovieMobileFragment
import com.tanasi.streamflix.fragments.movie.MovieMobileFragmentDirections
import com.tanasi.streamflix.fragments.movies.MoviesFragment
import com.tanasi.streamflix.fragments.movies.MoviesFragmentDirections
import com.tanasi.streamflix.fragments.movies.MoviesMobileFragment
import com.tanasi.streamflix.fragments.movies.MoviesMobileFragmentDirections
import com.tanasi.streamflix.fragments.people.PeopleFragment
import com.tanasi.streamflix.fragments.people.PeopleFragmentDirections
import com.tanasi.streamflix.fragments.people.PeopleMobileFragment
import com.tanasi.streamflix.fragments.people.PeopleMobileFragmentDirections
import com.tanasi.streamflix.fragments.player.PlayerFragment
import com.tanasi.streamflix.fragments.search.SearchFragment
import com.tanasi.streamflix.fragments.search.SearchFragmentDirections
import com.tanasi.streamflix.fragments.search.SearchMobileFragment
import com.tanasi.streamflix.fragments.search.SearchMobileFragmentDirections
import com.tanasi.streamflix.fragments.tv_show.TvShowFragment
import com.tanasi.streamflix.fragments.tv_show.TvShowFragmentDirections
import com.tanasi.streamflix.fragments.tv_show.TvShowMobileFragment
import com.tanasi.streamflix.fragments.tv_show.TvShowMobileFragmentDirections
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.ui.ShowOptionsDialog
import com.tanasi.streamflix.ui.ShowOptionsMobileDialog
import com.tanasi.streamflix.ui.SpacingItemDecoration
import com.tanasi.streamflix.utils.WatchNextUtils
import com.tanasi.streamflix.utils.dp
import com.tanasi.streamflix.utils.format
import com.tanasi.streamflix.utils.getCurrentFragment
import com.tanasi.streamflix.utils.toActivity

class MovieViewHolder(
    private val _binding: ViewBinding
) : RecyclerView.ViewHolder(
    _binding.root
) {

    private val context = itemView.context
    private val database = AppDatabase.getInstance(context)
    private lateinit var movie: Movie

    val childRecyclerView: RecyclerView?
        get() = when (_binding) {
            is ContentMovieCastsMobileBinding -> _binding.rvMovieCasts
            is ContentMovieCastsBinding -> _binding.hgvMovieCasts
            is ContentMovieRecommendationsMobileBinding -> _binding.rvMovieRecommendations
            is ContentMovieRecommendationsBinding -> _binding.hgvMovieRecommendations
            else -> null
        }

    fun bind(movie: Movie) {
        this.movie = movie

        when (_binding) {
            is ItemMovieMobileBinding -> displayMobileItem(_binding)
            is ItemMovieBinding -> displayItem(_binding)
            is ItemMovieGridMobileBinding -> displayGridMobileItem(_binding)
            is ItemMovieGridBinding -> displayGridItem(_binding)

            is ContentMovieMobileBinding -> displayMovieMobile(_binding)
            is ContentMovieBinding -> displayMovie(_binding)
            is ContentMovieCastsMobileBinding -> displayCastsMobile(_binding)
            is ContentMovieCastsBinding -> displayCasts(_binding)
            is ContentMovieRecommendationsMobileBinding -> displayRecommendationsMobile(_binding)
            is ContentMovieRecommendationsBinding -> displayRecommendations(_binding)
        }
    }


    private fun displayMobileItem(binding: ItemMovieMobileBinding) {
        database.movieDao().getById(movie.id)?.let { movieDb ->
            movie.isFavorite = movieDb.isFavorite
            movie.isWatched = movieDb.isWatched
            movie.watchHistory = movieDb.watchHistory
        }

        binding.root.apply {
            setOnClickListener {
                when (context.toActivity()?.getCurrentFragment()) {
                    is HomeMobileFragment -> findNavController().navigate(
                        HomeMobileFragmentDirections.actionHomeToMovie(
                            id = movie.id
                        )
                    )
                    is MovieMobileFragment -> findNavController().navigate(
                        MovieMobileFragmentDirections.actionMovieToMovie(
                            id = movie.id
                        )
                    )
                    is TvShowMobileFragment -> findNavController().navigate(
                        TvShowMobileFragmentDirections.actionTvShowToMovie(
                            id = movie.id
                        )
                    )
                }
            }
            setOnLongClickListener {
                ShowOptionsMobileDialog(context, movie)
                    .show()
                true
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

        binding.pbMovieProgress.apply {
            val watchHistory = movie.watchHistory

            progress = when {
                watchHistory != null -> (watchHistory.lastPlaybackPositionMillis * 100 / watchHistory.durationMillis.toDouble()).toInt()
                else -> 0
            }
            visibility = when {
                watchHistory != null -> View.VISIBLE
                else -> View.GONE
            }
        }

        binding.tvMovieTitle.text = movie.title
    }

    private fun displayItem(binding: ItemMovieBinding) {
        val program = WatchNextUtils.getProgram(context, movie.id)

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
                    is TvShowFragment -> findNavController().navigate(
                        TvShowFragmentDirections.actionTvShowToMovie(
                            id = movie.id
                        )
                    )
                }
            }
            setOnLongClickListener {
                ShowOptionsDialog(context, movie)
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
            progress = when {
                program != null -> (program.lastPlaybackPositionMillis * 100 / program.durationMillis.toDouble()).toInt()
                else -> 0
            }
            visibility = when {
                program != null -> View.VISIBLE
                else -> View.GONE
            }
        }

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

    private fun displayGridMobileItem(binding: ItemMovieGridMobileBinding) {
        database.movieDao().getById(movie.id)?.let { movieDb ->
            movie.isFavorite = movieDb.isFavorite
            movie.isWatched = movieDb.isWatched
            movie.watchHistory = movieDb.watchHistory
        }

        binding.root.apply {
            setOnClickListener {
                when (context.toActivity()?.getCurrentFragment()) {
                    is GenreMobileFragment -> findNavController().navigate(
                        GenreMobileFragmentDirections.actionGenreToMovie(
                            id = movie.id
                        )
                    )
                    is MoviesMobileFragment -> findNavController().navigate(
                        MoviesMobileFragmentDirections.actionMoviesToMovie(
                            id = movie.id
                        )
                    )
                    is PeopleMobileFragment -> findNavController().navigate(
                        PeopleMobileFragmentDirections.actionPeopleToMovie(
                            id = movie.id
                        )
                    )
                    is SearchMobileFragment -> findNavController().navigate(
                        SearchMobileFragmentDirections.actionSearchToMovie(
                            id = movie.id
                        )
                    )
                }
            }
            setOnLongClickListener {
                ShowOptionsMobileDialog(context, movie)
                    .show()
                true
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

        binding.pbMovieProgress.apply {
            val watchHistory = movie.watchHistory

            progress = when {
                watchHistory != null -> (watchHistory.lastPlaybackPositionMillis * 100 / watchHistory.durationMillis.toDouble()).toInt()
                else -> 0
            }
            visibility = when {
                watchHistory != null -> View.VISIBLE
                else -> View.GONE
            }
        }

        binding.tvMovieTitle.text = movie.title
    }

    private fun displayGridItem(binding: ItemMovieGridBinding) {
        val program = WatchNextUtils.getProgram(context, movie.id)

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
                    is PeopleFragment -> findNavController().navigate(
                        PeopleFragmentDirections.actionPeopleToMovie(
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
            setOnLongClickListener {
                ShowOptionsDialog(context, movie)
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
            .load(movie.poster)
            .centerCrop()
            .into(binding.ivMoviePoster)

        binding.pbMovieProgress.apply {
            progress = when {
                program != null -> (program.lastPlaybackPositionMillis * 100 / program.durationMillis.toDouble()).toInt()
                else -> 0
            }
            visibility = when {
                program != null -> View.VISIBLE
                else -> View.GONE
            }
        }

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


    private fun displayMovieMobile(binding: ContentMovieMobileBinding) {
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

        binding.tvMovieGenres.apply {
            text = movie.genres.joinToString(", ") { it.name }
            visibility = when {
                movie.genres.isEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.tvMovieOverview.text = movie.overview

        binding.btnMovieWatchNow.apply {
            setOnClickListener {
                findNavController().navigate(
                    MovieMobileFragmentDirections.actionMovieToPlayer(
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
            val watchHistory = movie.watchHistory

            progress = when {
                watchHistory != null -> (watchHistory.lastPlaybackPositionMillis * 100 / watchHistory.durationMillis.toDouble()).toInt()
                else -> 0
            }
            visibility = when {
                watchHistory != null -> View.VISIBLE
                else -> View.GONE
            }
        }

        binding.btnMovieTrailer.setOnClickListener {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(movie.trailer)
                )
            )
        }

        binding.btnMovieFavorite.apply {
            fun Boolean.drawable() = when (this) {
                true -> R.drawable.ic_favorite_enable
                false -> R.drawable.ic_favorite_disable
            }

            setOnClickListener {
                database.movieDao().updateFavorite(
                    id = movie.id,
                    isFavorite = !movie.isFavorite
                )
                movie.isFavorite = !movie.isFavorite

                setImageDrawable(
                    ContextCompat.getDrawable(context, movie.isFavorite.drawable())
                )
            }

            setImageDrawable(
                ContextCompat.getDrawable(context, movie.isFavorite.drawable())
            )
        }
    }

    private fun displayMovie(binding: ContentMovieBinding) {
        val program = WatchNextUtils.getProgram(context, movie.id)

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

        binding.tvMovieGenres.apply {
            text = movie.genres.joinToString(", ") { it.name }
            visibility = when {
                movie.genres.isEmpty() -> View.GONE
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
                    Uri.parse(movie.trailer)
                )
            )
        }

        binding.btnMovieFavorite.apply {
            fun Boolean.drawable() = when (this) {
                true -> R.drawable.ic_favorite_enable
                false -> R.drawable.ic_favorite_disable
            }

            setOnClickListener {
                database.movieDao().updateFavorite(
                    id = movie.id,
                    isFavorite = !movie.isFavorite
                )
                movie.isFavorite = !movie.isFavorite

                setImageDrawable(
                    ContextCompat.getDrawable(context, movie.isFavorite.drawable())
                )
            }

            setImageDrawable(
                ContextCompat.getDrawable(context, movie.isFavorite.drawable())
            )
        }
    }

    private fun displayCastsMobile(binding: ContentMovieCastsMobileBinding) {
        binding.rvMovieCasts.apply {
            adapter = AppAdapter().apply {
                items.addAll(movie.cast.onEach {
                    it.itemType = AppAdapter.Type.PEOPLE_MOBILE_ITEM
                })
            }
            if (itemDecorationCount == 0) {
                addItemDecoration(SpacingItemDecoration(20.dp(context)))
            }
        }
    }

    private fun displayCasts(binding: ContentMovieCastsBinding) {
        binding.hgvMovieCasts.apply {
            setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
            adapter = AppAdapter().apply {
                items.addAll(movie.cast)
            }
            setItemSpacing(80)
        }
    }

    private fun displayRecommendationsMobile(binding: ContentMovieRecommendationsMobileBinding) {
        binding.rvMovieRecommendations.apply {
            adapter = AppAdapter().apply {
                items.addAll(movie.recommendations.onEach {
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

    private fun displayRecommendations(binding: ContentMovieRecommendationsBinding) {
        binding.hgvMovieRecommendations.apply {
            setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
            adapter = AppAdapter().apply {
                items.addAll(movie.recommendations.onEach {
                    when (it) {
                        is Movie -> it.itemType = AppAdapter.Type.MOVIE_ITEM
                        is TvShow -> it.itemType = AppAdapter.Type.TV_SHOW_ITEM
                    }
                })
            }
            setItemSpacing(20)
        }
    }
}