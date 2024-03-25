package com.tanasi.streamflix.fragments.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.tvprovider.media.tv.TvContractCompat
import com.bumptech.glide.Glide
import com.tanasi.streamflix.R
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.database.AppDatabase
import com.tanasi.streamflix.databinding.FragmentHomeTvBinding
import com.tanasi.streamflix.models.Category
import com.tanasi.streamflix.models.Episode
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.models.Season
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.models.WatchItem
import com.tanasi.streamflix.utils.WatchNextUtils

class HomeTvFragment : Fragment() {

    private var _binding: FragmentHomeTvBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<HomeViewModel>()

    private lateinit var database: AppDatabase

    private val appAdapter = AppAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeTvBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = AppDatabase.getInstance(requireContext())

        WatchNextUtils.programs(requireContext())
            .forEach { program ->
                when (program.type) {
                    TvContractCompat.PreviewPrograms.TYPE_MOVIE -> {
                        database.movieDao().getById(program.contentId)
                            ?.let { movieDb ->
                                movieDb.watchHistory = WatchItem.WatchHistory(
                                    program.lastEngagementTimeUtcMillis,
                                    program.lastPlaybackPositionMillis.toLong(),
                                    program.durationMillis.toLong(),
                                )
                                database.movieDao().update(movieDb)
                            }
                            ?: database.movieDao().insert(Movie(
                                id = program.contentId,
                                title = program.title,
                                released = program.releaseDate,
                                poster = program.posterArtUri?.toString(),
                            ).also { movie ->
                                movie.watchHistory = WatchItem.WatchHistory(
                                    program.lastEngagementTimeUtcMillis,
                                    program.lastPlaybackPositionMillis.toLong(),
                                    program.durationMillis.toLong(),
                                )
                            })
                    }
                    TvContractCompat.PreviewPrograms.TYPE_TV_EPISODE -> {
                        database.episodeDao().getById(program.contentId)
                            ?.let { episodeDb ->
                                episodeDb.watchHistory = WatchItem.WatchHistory(
                                    program.lastEngagementTimeUtcMillis,
                                    program.lastPlaybackPositionMillis.toLong(),
                                    program.durationMillis.toLong(),
                                )
                                database.episodeDao().update(episodeDb)
                            }
                            ?: database.episodeDao().insert(Episode(
                                id = program.contentId,
                                number = program.episodeNumber?.toIntOrNull() ?: 0,
                                title = program.episodeTitle,

                                tvShow = TvShow(
                                    id = program.seriesId ?: "",
                                    title = program.title ?: "",
                                    poster = program.posterArtUri?.toString(),
                                ),
                                season = Season(
                                    id = "",
                                    number = program.seasonNumber?.toIntOrNull() ?: 0,
                                    title = program.seasonTitle,
                                ),
                            ).also { episode ->
                                episode.watchHistory = WatchItem.WatchHistory(
                                    program.lastEngagementTimeUtcMillis,
                                    program.lastPlaybackPositionMillis.toLong(),
                                    program.durationMillis.toLong(),
                                )
                            })
                    }
                    else -> {}
                }

                WatchNextUtils.deleteProgramById(requireContext(), program.id)
            }

        initializeHome()

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                HomeViewModel.State.Loading -> binding.isLoading.apply {
                    root.visibility = View.VISIBLE
                    pbIsLoading.visibility = View.VISIBLE
                    gIsLoadingRetry.visibility = View.GONE
                }
                is HomeViewModel.State.SuccessLoading -> {
                    displayHome(state.categories)
                    binding.vgvHome.visibility = View.VISIBLE
                    binding.isLoading.root.visibility = View.GONE
                }
                is HomeViewModel.State.FailedLoading -> {
                    Toast.makeText(
                        requireContext(),
                        state.error.message ?: "",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.isLoading.apply {
                        pbIsLoading.visibility = View.GONE
                        gIsLoadingRetry.visibility = View.VISIBLE
                        btnIsLoadingRetry.setOnClickListener {
                            viewModel.getHome()
                        }
                        binding.vgvHome.visibility = View.GONE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        appAdapter.onSaveInstanceState(binding.vgvHome)
        _binding = null
    }


    fun updateBackground(uri: String?) {
        Glide.with(requireContext())
            .load(uri)
            .into(binding.ivHomeBackground)
    }

    fun refresh() {
        appAdapter.onSaveInstanceState(binding.vgvHome)
        when (val state = viewModel.state.value) {
            is HomeViewModel.State.SuccessLoading -> displayHome(state.categories)
            else -> {}
        }
    }

    private fun initializeHome() {
        binding.vgvHome.apply {
            adapter = appAdapter
            setItemSpacing(resources.getDimension(R.dimen.home_spacing).toInt() * 2)
        }

        binding.root.requestFocus()
    }

    private fun displayHome(categories: List<Category>) {
        appAdapter.submitList(listOfNotNull(
            categories
                .find { it.name == Category.FEATURED }
                ?.also { it.itemType = AppAdapter.Type.CATEGORY_TV_SWIPER },

            Category(
                name = getString(R.string.home_continue_watching),
                list = listOf(
                    database.movieDao().getWatchingMovies(),
                    database.episodeDao().getWatchingEpisodes().onEach { episode ->
                        episode.tvShow = episode.tvShow?.let { database.tvShowDao().getById(it.id) }
                        episode.season = episode.season?.let { database.seasonDao().getById(it.id) }
                    },
                ).flatten().sortedByDescending { it.watchHistory?.lastEngagementTimeUtcMillis }
            ).takeIf { it.list.isNotEmpty() }?.also {
                it.list.onEach { show ->
                    when (show) {
                        is Movie -> show.itemType = AppAdapter.Type.MOVIE_TV_ITEM
                        is Episode -> show.itemType = AppAdapter.Type.EPISODE_CONTINUE_WATCHING_TV_ITEM
                    }
                }
                it.itemSpacing = resources.getDimension(R.dimen.home_spacing).toInt()
                it.itemType = AppAdapter.Type.CATEGORY_TV_ITEM
            },

            Category(
                name = getString(R.string.home_favorite_movies),
                list = database.movieDao().getFavorites()
                    .reversed(),
            ).takeIf { it.list.isNotEmpty() }?.also {
                it.list.onEach { show ->
                    when (show) {
                        is Movie -> show.itemType = AppAdapter.Type.MOVIE_TV_ITEM
                        is TvShow -> show.itemType = AppAdapter.Type.TV_SHOW_TV_ITEM
                    }
                }
                it.itemSpacing = resources.getDimension(R.dimen.home_spacing).toInt()
                it.itemType = AppAdapter.Type.CATEGORY_TV_ITEM
            },

            Category(
                name = getString(R.string.home_favorite_tv_shows),
                list = database.tvShowDao().getFavorites()
                    .reversed(),
            ).takeIf { it.list.isNotEmpty() }?.also {
                it.list.onEach { show ->
                    when (show) {
                        is Movie -> show.itemType = AppAdapter.Type.MOVIE_TV_ITEM
                        is TvShow -> show.itemType = AppAdapter.Type.TV_SHOW_TV_ITEM
                    }
                }
                it.itemSpacing = resources.getDimension(R.dimen.home_spacing).toInt()
                it.itemType = AppAdapter.Type.CATEGORY_TV_ITEM
            },
        ) + categories
            .filter { it.name != Category.FEATURED && it.list.isNotEmpty() }
            .onEach { category ->
                category.list.onEach { show ->
                    when (show) {
                        is Movie -> show.itemType = AppAdapter.Type.MOVIE_TV_ITEM
                        is TvShow -> show.itemType = AppAdapter.Type.TV_SHOW_TV_ITEM
                    }
                }
                category.itemSpacing = resources.getDimension(R.dimen.home_spacing).toInt()
                category.itemType = AppAdapter.Type.CATEGORY_TV_ITEM
            }
        )
    }
}