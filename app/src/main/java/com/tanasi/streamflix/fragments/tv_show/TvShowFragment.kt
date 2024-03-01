package com.tanasi.streamflix.fragments.tv_show

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.database.AppDatabase
import com.tanasi.streamflix.databinding.FragmentTvShowBinding
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.utils.WatchNextUtils
import com.tanasi.streamflix.utils.viewModelsFactory

class TvShowFragment : Fragment() {

    private var _binding: FragmentTvShowBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<TvShowFragmentArgs>()
    private val viewModel by viewModelsFactory { TvShowViewModel(args.id) }

    private lateinit var database: AppDatabase

    private val appAdapter = AppAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTvShowBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = AppDatabase.getInstance(requireContext())

        initializeTvShow()

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                TvShowViewModel.State.Loading -> binding.isLoading.apply {
                    root.visibility = View.VISIBLE
                    pbIsLoading.visibility = View.VISIBLE
                    gIsLoadingRetry.visibility = View.GONE
                }
                is TvShowViewModel.State.SuccessLoading -> {
                    val episodes = database.episodeDao().getEpisodesByTvShowId(state.tvShow.id)
                    state.tvShow.seasons.onEach { season ->
                        season.episodes = episodes.filter { it.season?.id == season.id }
                    }

                    if (episodes.isEmpty()) {
                        state.tvShow.seasons.firstOrNull()?.let {
                            viewModel.getSeason(state.tvShow, it)
                        }
                    } else {
                        val season = state.tvShow.seasons.let { seasons ->
                            seasons
                                .lastOrNull { season ->
                                    season.episodes.lastOrNull()?.isWatched == true ||
                                            season.episodes.any { it.isWatched }
                                }?.let { season ->
                                    if (season.episodes.lastOrNull()?.isWatched == true) {
                                        val next = seasons.getOrNull(seasons.indexOf(season) + 1)
                                        next ?: season
                                    } else season
                                }
                                ?: seasons.firstOrNull { season ->
                                    season.episodes.isEmpty() ||
                                            season.episodes.lastOrNull()?.isWatched == false
                                }
                        }

                        val episodeIndex = WatchNextUtils.programs(requireContext())
                            .filter { it.seriesId == state.tvShow.id }
                            .sortedByDescending { it.lastEngagementTimeUtcMillis }
                            .let { programs ->
                                val program = programs.find { program ->
                                    episodes.any { it.id == program.contentId }
                                }
                                episodes.indexOfFirst { it.id == program?.contentId }.takeIf { it != -1 }
                            }
                            ?: episodes.indexOfLast { it.isWatched }
                                .takeIf { it + 1 < episodes.size }
                                ?.let { it + 1 }

                        if (
                            episodeIndex == null &&
                            season != null &&
                            (season.episodes.isEmpty() || state.tvShow.seasons.lastOrNull() == season)
                        ) {
                            viewModel.getSeason(state.tvShow, season)
                        }
                    }

                    displayTvShow(state.tvShow)
                    binding.isLoading.root.visibility = View.GONE
                }
                is TvShowViewModel.State.FailedLoading -> {
                    Toast.makeText(
                        requireContext(),
                        state.error.message ?: "",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.isLoading.apply {
                        pbIsLoading.visibility = View.GONE
                        gIsLoadingRetry.visibility = View.VISIBLE
                        btnIsLoadingRetry.setOnClickListener {
                            viewModel.getTvShow(args.id)
                        }
                        btnIsLoadingRetry.requestFocus()
                    }
                }
            }
        }

        viewModel.seasonState.observe(viewLifecycleOwner) { state ->
            when (state) {
                TvShowViewModel.SeasonState.Loading -> {}
                is TvShowViewModel.SeasonState.SuccessLoading -> {
                    val episodes = database.episodeDao().getEpisodesByTvShowId(state.tvShow.id)
                    state.episodes.onEach { episode ->
                        episode.isWatched = episodes.find { it.id == episode.id }?.isWatched
                            ?: false

                        episode.tvShow = state.tvShow
                        episode.season = state.season.takeIf { it.number != 0 }
                    }
                    database.episodeDao().insertAll(state.episodes)
                    appAdapter.notifyItemChanged(0)
                }
                is TvShowViewModel.SeasonState.FailedLoading -> {
                    Toast.makeText(
                        requireContext(),
                        state.error.message ?: "",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        appAdapter.onSaveInstanceState(binding.vgvTvShow)
        _binding = null
    }


    private fun initializeTvShow() {
        binding.vgvTvShow.apply {
            adapter = appAdapter
            setItemSpacing(80)
        }
    }

    private fun displayTvShow(tvShow: TvShow) {
        database.tvShowDao().getTvShowById(tvShow.id)?.let {
            tvShow.isFavorite = it.isFavorite
        }
        database.tvShowDao().insert(tvShow)

        Glide.with(requireContext())
            .load(tvShow.banner)
            .into(binding.ivTvShowBanner)

        appAdapter.submitList(listOfNotNull(
            tvShow.apply { itemType = AppAdapter.Type.TV_SHOW },

            tvShow.takeIf { it.seasons.isNotEmpty() }
                ?.clone()
                ?.apply {
                    seasons.onEach {
                        it.tvShow = tvShow
                    }
                    database.seasonDao().insertAll(seasons)
                    itemType = AppAdapter.Type.TV_SHOW_SEASONS
                },

            tvShow.takeIf { it.cast.isNotEmpty() }
                ?.clone()
                ?.apply { itemType = AppAdapter.Type.TV_SHOW_CASTS },

            tvShow.takeIf { it.recommendations.isNotEmpty() }
                ?.clone()
                ?.apply { itemType = AppAdapter.Type.TV_SHOW_RECOMMENDATIONS },
        ))
    }
}