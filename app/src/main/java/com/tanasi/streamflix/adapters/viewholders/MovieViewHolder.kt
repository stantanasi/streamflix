package com.tanasi.streamflix.adapters.viewholders

import android.app.AlertDialog
import android.content.Context // Added for SharedPreferences
import android.content.Intent
import android.content.SharedPreferences // Added for SharedPreferences
import android.content.pm.PackageManager
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.tanasi.streamflix.R
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.database.AppDatabase
import com.tanasi.streamflix.databinding.ContentMovieCastMobileBinding
import com.tanasi.streamflix.databinding.ContentMovieCastTvBinding
import com.tanasi.streamflix.databinding.ContentMovieMobileBinding
import com.tanasi.streamflix.databinding.ContentMovieRecommendationsMobileBinding
import com.tanasi.streamflix.databinding.ContentMovieRecommendationsTvBinding
import com.tanasi.streamflix.databinding.ContentMovieTvBinding
import com.tanasi.streamflix.databinding.ItemCategorySwiperMobileBinding
import com.tanasi.streamflix.databinding.ItemMovieGridMobileBinding
import com.tanasi.streamflix.databinding.ItemMovieGridTvBinding
import com.tanasi.streamflix.databinding.ItemMovieMobileBinding
import com.tanasi.streamflix.databinding.ItemMovieTvBinding
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
import com.tanasi.streamflix.fragments.movies.MoviesMobileFragment
import com.tanasi.streamflix.fragments.movies.MoviesMobileFragmentDirections
import com.tanasi.streamflix.fragments.movies.MoviesTvFragment
import com.tanasi.streamflix.fragments.movies.MoviesTvFragmentDirections
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

class MovieViewHolder(
    private val _binding: ViewBinding
) : RecyclerView.ViewHolder(
    _binding.root
) {

    private val context = itemView.context
    private val database = AppDatabase.getInstance(context)
    private lateinit var movie: Movie
    private val TAG = "TrailerChoiceDebug" // Logging Tag

    companion object {
        private const val PREFS_NAME = "TrailerPlayerPrefs"
        private const val KEY_PREFERRED_PLAYER = "preferred_player"
        private const val PLAYER_YOUTUBE = "youtube"
        private const val PLAYER_SMARTTUBE = "smarttube"
    }

    val childRecyclerView: RecyclerView?
        get() = when (_binding) {
            is ContentMovieCastMobileBinding -> _binding.rvMovieCast
            is ContentMovieCastTvBinding -> _binding.hgvMovieCast
            is ContentMovieRecommendationsMobileBinding -> _binding.rvMovieRecommendations
            is ContentMovieRecommendationsTvBinding -> _binding.hgvMovieRecommendations
            else -> null
        }

    fun bind(movie: Movie) {
        this.movie = movie

        when (_binding) {
            is ItemMovieMobileBinding -> displayMobileItem(_binding)
            is ItemMovieTvBinding -> displayTvItem(_binding)
            is ItemMovieGridMobileBinding -> displayGridMobileItem(_binding)
            is ItemMovieGridTvBinding -> displayGridTvItem(_binding)
            is ItemCategorySwiperMobileBinding -> displaySwiperMobileItem(_binding)

            is ContentMovieMobileBinding -> displayMovieMobile(_binding)
            is ContentMovieTvBinding -> displayMovieTv(_binding)
            is ContentMovieCastMobileBinding -> displayCastMobile(_binding)
            is ContentMovieCastTvBinding -> displayCastTv(_binding)
            is ContentMovieRecommendationsMobileBinding -> displayRecommendationsMobile(_binding)
            is ContentMovieRecommendationsTvBinding -> displayRecommendationsTv(_binding)
        }
    }


    private fun displayMobileItem(binding: ItemMovieMobileBinding) {
        binding.root.apply {
            setOnClickListener {
                when (context.toActivity()?.getCurrentFragment()) {
                    is HomeMobileFragment -> {
                        findNavController().navigate(
                            HomeMobileFragmentDirections.actionHomeToMovie(
                                id = movie.id
                            )
                        )
                        if (movie.itemType == AppAdapter.Type.MOVIE_CONTINUE_WATCHING_MOBILE_ITEM) {
                            findNavController().navigate(
                                MovieMobileFragmentDirections.actionMovieToPlayer(
                                    id = movie.id,
                                    title = movie.title,
                                    subtitle = movie.released?.format("yyyy") ?: "",
                                    videoType = Video.Type.Movie(
                                        id = movie.id,
                                        title = movie.title,
                                        releaseDate = movie.released?.format("yyyy-MM-dd") ?: "",
                                        poster = movie.poster ?: "",
                                    ),
                                )
                            )
                        }
                    }
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
            .transition(DrawableTransitionOptions.withCrossFade())
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

    private fun displayTvItem(binding: ItemMovieTvBinding) {
        binding.root.apply {
            setOnClickListener {
                when (context.toActivity()?.getCurrentFragment()) {
                    is HomeTvFragment -> {
                        findNavController().navigate(
                            HomeTvFragmentDirections.actionHomeToMovie(
                                id = movie.id
                            )
                        )
                        if (movie.itemType == AppAdapter.Type.MOVIE_CONTINUE_WATCHING_TV_ITEM) {
                            findNavController().navigate(
                                MovieTvFragmentDirections.actionMovieToPlayer(
                                    id = movie.id,
                                    title = movie.title,
                                    subtitle = movie.released?.format("yyyy") ?: "",
                                    videoType = Video.Type.Movie(
                                        id = movie.id,
                                        title = movie.title,
                                        releaseDate = movie.released?.format("yyyy-MM-dd") ?: "",
                                        poster = movie.poster ?: "",
                                    ),
                                )
                            )
                        }
                    }
                    is MovieTvFragment -> findNavController().navigate(
                        MovieTvFragmentDirections.actionMovieToMovie(
                            id = movie.id
                        )
                    )
                    is TvShowTvFragment -> findNavController().navigate(
                        TvShowTvFragmentDirections.actionTvShowToMovie(
                            id = movie.id
                        )
                    )
                }
            }
            setOnLongClickListener {
                ShowOptionsTvDialog(context, movie)
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
                        is HomeTvFragment -> fragment.updateBackground(movie.banner)
                    }
                }
            }
        }

        Glide.with(context)
            .load(movie.poster)
            .fallback(R.drawable.glide_fallback_cover)
            .centerCrop()
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.ivMoviePoster)

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
            .transition(DrawableTransitionOptions.withCrossFade())
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

    private fun displayGridTvItem(binding: ItemMovieGridTvBinding) {
        binding.root.apply {
            setOnClickListener {
                when (context.toActivity()?.getCurrentFragment()) {
                    is GenreTvFragment -> findNavController().navigate(
                        GenreTvFragmentDirections.actionGenreToMovie(
                            id = movie.id
                        )
                    )
                    is MoviesTvFragment -> findNavController().navigate(
                        MoviesTvFragmentDirections.actionMoviesToMovie(
                            id = movie.id
                        )
                    )
                    is PeopleTvFragment -> findNavController().navigate(
                        PeopleTvFragmentDirections.actionPeopleToMovie(
                            id = movie.id
                        )
                    )
                    is SearchTvFragment -> findNavController().navigate(
                        SearchTvFragmentDirections.actionSearchToMovie(
                            id = movie.id
                        )
                    )
                }
            }
            setOnLongClickListener {
                ShowOptionsTvDialog(context, movie)
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
            .fallback(R.drawable.glide_fallback_cover)
            .centerCrop()
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.ivMoviePoster)

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

    private fun displaySwiperMobileItem(binding: ItemCategorySwiperMobileBinding) {
        Glide.with(context)
            .load(movie.banner)
            .centerCrop()
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.ivSwiperBackground)

        binding.tvSwiperTitle.text = movie.title

        binding.tvSwiperTvShowLastEpisode.text = context.getString(R.string.movie_item_type)

        binding.tvSwiperQuality.apply {
            text = movie.quality
            visibility = when {
                text.isNullOrEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.tvSwiperReleased.apply {
            text = movie.released?.format("yyyy")
            visibility = when {
                text.isNullOrEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.tvSwiperRating.apply {
            text = movie.rating?.let { String.format(Locale.ROOT, "%.1f", it) } ?: "N/A"
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

            text = movie.overview
        }

        binding.btnSwiperWatchNow.apply {
            setOnClickListener {
                findNavController().navigate(
                    HomeMobileFragmentDirections.actionHomeToMovie(
                        id = movie.id,
                    )
                )
            }
        }

        binding.pbSwiperProgress.apply {
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
    }


    private fun displayMovieMobile(binding: ContentMovieMobileBinding) {
        binding.ivMoviePoster.run {
            Glide.with(context)
                .load(movie.poster)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(this)
            visibility = when {
                movie.poster.isNullOrEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.tvMovieTitle.text = movie.title

        binding.tvMovieRating.text = movie.rating?.let { String.format(Locale.ROOT, "%.1f", it) } ?: "N/A"

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
                        videoType = Video.Type.Movie(
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

        binding.btnMovieTrailer.apply {
            val trailer = movie.trailer
            Log.d(TAG, "MovieMobile: btnMovieTrailer.apply called. Trailer URL: $trailer")

            setOnClickListener {
                Log.d(TAG, "MovieMobile: setOnClickListener called.")
                if (trailer == null) {
                    Log.d(TAG, "MovieMobile: Trailer is null, doing nothing.")
                    return@setOnClickListener
                }

                val youtubeIntent = Intent(Intent.ACTION_VIEW, trailer.toUri())
                val smartTubeIntent = Intent(Intent.ACTION_VIEW, trailer.toUri())
                smartTubeIntent.setPackage("com.teamsmart.videomanager.tv")
                Log.d(TAG, "MovieMobile: Intents created. YouTube: $youtubeIntent, SmartTube: $smartTubeIntent")

                val smartTubeInstalled = try {
                    context.packageManager.getPackageInfo("com.teamsmart.videomanager.tv", 0)
                    Log.d(TAG, "MovieMobile: SmartTube package info found.")
                    true
                } catch (_: PackageManager.NameNotFoundException) {
                    Log.d(TAG, "MovieMobile: SmartTube package info NOT found.")
                    false
                }
                Log.d(TAG, "MovieMobile: smartTubeInstalled = $smartTubeInstalled")

                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val preferredPlayer = prefs.getString(KEY_PREFERRED_PLAYER, null)
                Log.d(TAG, "MovieMobile: Preferred player from prefs: $preferredPlayer")

                if (preferredPlayer == PLAYER_SMARTTUBE && smartTubeInstalled) {
                    Log.d(TAG, "MovieMobile: Preferred player is SmartTube and it's installed. Launching SmartTube.")
                    context.startActivity(smartTubeIntent)
                } else if (preferredPlayer == PLAYER_SMARTTUBE && !smartTubeInstalled) {
                    Log.d(TAG, "MovieMobile: Preferred player was SmartTube, but it's not installed. Launching YouTube.")
                    context.startActivity(youtubeIntent) // Launch YouTube but don't change preference
                } else if (preferredPlayer == PLAYER_YOUTUBE) {
                    Log.d(TAG, "MovieMobile: Preferred player is YouTube. Launching YouTube.")
                    context.startActivity(youtubeIntent)
                } else {
                    // No valid preference or SmartTube preferred but not installed the first time
                    if (smartTubeInstalled) {
                        Log.d(TAG, "MovieMobile: No valid preference or SmartTube not installed previously. SmartTube IS installed now. Showing dialog.")
                        AlertDialog.Builder(context)
                            .setTitle(context.getString(R.string.watch_trailer_with))
                            .setItems(
                                arrayOf(
                                    context.getString(R.string.youtube),
                                    context.getString(R.string.smarttube)
                                )
                            ) { _, which ->
                                Log.d(TAG, "MovieMobile: AlertDialog item selected: $which")
                                val editor = prefs.edit()
                                when (which) {
                                    0 -> {
                                        Log.d(TAG, "MovieMobile: Launching YouTube and saving preference.")
                                        editor.putString(KEY_PREFERRED_PLAYER, PLAYER_YOUTUBE)
                                        context.startActivity(youtubeIntent)
                                    }
                                    1 -> {
                                        Log.d(TAG, "MovieMobile: Launching SmartTube and saving preference.")
                                        editor.putString(KEY_PREFERRED_PLAYER, PLAYER_SMARTTUBE)
                                        context.startActivity(smartTubeIntent)
                                    }
                                }
                                editor.apply()
                            }
                            .show()
                    } else {
                        Log.d(TAG, "MovieMobile: No preference and SmartTube not installed. Launching YouTube directly.")
                        context.startActivity(youtubeIntent)
                        // Optionally, save YouTube as default here if desired
                        // prefs.edit().putString(KEY_PREFERRED_PLAYER, PLAYER_YOUTUBE).apply()
                    }
                }
            }

            visibility = when {
                trailer != null -> View.VISIBLE
                else -> {
                    Log.d(TAG, "MovieMobile: Trailer is null, setting button visibility to GONE.")
                    View.GONE
                }
            }
        }

        binding.btnMovieFavorite.apply {
            fun Boolean.drawable() = when (this) {
                true -> R.drawable.ic_favorite_enable
                false -> R.drawable.ic_favorite_disable
            }

            setOnClickListener {
                movie.isFavorite = !movie.isFavorite
                database.movieDao().update(movie)

                setImageDrawable(
                    ContextCompat.getDrawable(context, movie.isFavorite.drawable())
                )
            }

            setImageDrawable(
                ContextCompat.getDrawable(context, movie.isFavorite.drawable())
            )
        }
    }

    private fun displayMovieTv(binding: ContentMovieTvBinding) {
        binding.ivMoviePoster.run {
            Glide.with(context)
                .load(movie.poster)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(this)
            visibility = when {
                movie.poster.isNullOrEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.tvMovieTitle.text = movie.title

        binding.tvMovieRating.text = movie.rating?.let { String.format(Locale.ROOT, "%.1f", it) } ?: "N/A"

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
                    MovieTvFragmentDirections.actionMovieToPlayer(
                        id = movie.id,
                        title = movie.title,
                        subtitle = movie.released?.format("yyyy") ?: "",
                        videoType = Video.Type.Movie(
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

        binding.btnMovieTrailer.apply {
            val trailer = movie.trailer
            Log.d(TAG, "MovieTv: btnMovieTrailer.apply called. Trailer URL: $trailer")

            setOnClickListener {
                Log.d(TAG, "MovieTv: setOnClickListener called.")
                if (trailer == null) {
                    Log.d(TAG, "MovieTv: Trailer is null, doing nothing.")
                    return@setOnClickListener
                }

                val youtubeIntent = Intent(Intent.ACTION_VIEW, trailer.toUri())
                val smartTubeIntent = Intent(Intent.ACTION_VIEW, trailer.toUri())
                smartTubeIntent.setPackage("com.teamsmart.videomanager.tv")
                Log.d(TAG, "MovieTv: Intents created. YouTube: $youtubeIntent, SmartTube: $smartTubeIntent")

                val smartTubeInstalled = try {
                    context.packageManager.getPackageInfo("com.teamsmart.videomanager.tv", 0)
                    Log.d(TAG, "MovieTv: SmartTube package info found.")
                    true
                } catch (_: PackageManager.NameNotFoundException) {
                    Log.d(TAG, "MovieTv: SmartTube package info NOT found.")
                    false
                }
                Log.d(TAG, "MovieTv: smartTubeInstalled = $smartTubeInstalled")

                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val preferredPlayer = prefs.getString(KEY_PREFERRED_PLAYER, null)
                Log.d(TAG, "MovieTv: Preferred player from prefs: $preferredPlayer")

                if (preferredPlayer == PLAYER_SMARTTUBE && smartTubeInstalled) {
                    Log.d(TAG, "MovieTv: Preferred player is SmartTube and it's installed. Launching SmartTube.")
                    context.startActivity(smartTubeIntent)
                } else if (preferredPlayer == PLAYER_SMARTTUBE && !smartTubeInstalled) {
                    Log.d(TAG, "MovieTv: Preferred player was SmartTube, but it's not installed. Launching YouTube.")
                    context.startActivity(youtubeIntent) // Launch YouTube but don't change preference
                } else if (preferredPlayer == PLAYER_YOUTUBE) {
                    Log.d(TAG, "MovieTv: Preferred player is YouTube. Launching YouTube.")
                    context.startActivity(youtubeIntent)
                } else {
                    // No valid preference or SmartTube preferred but not installed the first time
                    if (smartTubeInstalled) {
                        Log.d(TAG, "MovieTv: No valid preference or SmartTube not installed previously. SmartTube IS installed now. Showing dialog.")
                        AlertDialog.Builder(context)
                            .setTitle(context.getString(R.string.watch_trailer_with))
                            .setItems(
                                arrayOf(
                                    context.getString(R.string.youtube),
                                    context.getString(R.string.smarttube)
                                )
                            ) { _, which ->
                                Log.d(TAG, "MovieTv: AlertDialog item selected: $which")
                                val editor = prefs.edit()
                                when (which) {
                                    0 -> {
                                        Log.d(TAG, "MovieTv: Launching YouTube and saving preference.")
                                        editor.putString(KEY_PREFERRED_PLAYER, PLAYER_YOUTUBE)
                                        context.startActivity(youtubeIntent)
                                    }
                                    1 -> {
                                        Log.d(TAG, "MovieTv: Launching SmartTube and saving preference.")
                                        editor.putString(KEY_PREFERRED_PLAYER, PLAYER_SMARTTUBE)
                                        context.startActivity(smartTubeIntent)
                                    }
                                }
                                editor.apply()
                            }
                            .show()
                    } else {
                        Log.d(TAG, "MovieTv: No preference and SmartTube not installed. Launching YouTube directly.")
                        context.startActivity(youtubeIntent)
                        // Optionally, save YouTube as default here if desired
                        // prefs.edit().putString(KEY_PREFERRED_PLAYER, PLAYER_YOUTUBE).apply()
                    }
                }
            }

            visibility = when {
                trailer != null -> View.VISIBLE
                else -> {
                    Log.d(TAG, "MovieTv: Trailer is null, setting button visibility to GONE.")
                    View.GONE
                }
            }
        }

        binding.btnMovieFavorite.apply {
            fun Boolean.drawable() = when (this) {
                true -> R.drawable.ic_favorite_enable
                false -> R.drawable.ic_favorite_disable
            }

            setOnClickListener {
                movie.isFavorite = !movie.isFavorite
                database.movieDao().update(movie)

                setImageDrawable(
                    ContextCompat.getDrawable(context, movie.isFavorite.drawable())
                )
            }

            setImageDrawable(
                ContextCompat.getDrawable(context, movie.isFavorite.drawable())
            )
        }
    }

    private fun displayCastMobile(binding: ContentMovieCastMobileBinding) {
        binding.rvMovieCast.apply {
            adapter = AppAdapter().apply {
                submitList(movie.cast.onEach {
                    it.itemType = AppAdapter.Type.PEOPLE_MOBILE_ITEM
                })
            }
            if (itemDecorationCount == 0) {
                addItemDecoration(SpacingItemDecoration(20.dp(context)))
            }
        }
    }

    private fun displayCastTv(binding: ContentMovieCastTvBinding) {
        binding.hgvMovieCast.apply {
            setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
            adapter = AppAdapter().apply {
                submitList(movie.cast.onEach {
                    it.itemType = AppAdapter.Type.PEOPLE_TV_ITEM
                })
            }
            setItemSpacing(80)
        }
    }

    private fun displayRecommendationsMobile(binding: ContentMovieRecommendationsMobileBinding) {
        binding.rvMovieRecommendations.apply {
            adapter = AppAdapter().apply {
                submitList(movie.recommendations.onEach {
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

    private fun displayRecommendationsTv(binding: ContentMovieRecommendationsTvBinding) {
        binding.hgvMovieRecommendations.apply {
            setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
            adapter = AppAdapter().apply {
                submitList(movie.recommendations.onEach {
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
