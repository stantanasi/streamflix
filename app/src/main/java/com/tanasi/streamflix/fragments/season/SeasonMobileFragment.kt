package com.tanasi.streamflix.fragments.season

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.database.AppDatabase
import com.tanasi.streamflix.databinding.FragmentSeasonMobileBinding
import com.tanasi.streamflix.models.Episode
import com.tanasi.streamflix.models.Season
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.ui.SpacingItemDecoration
import com.tanasi.streamflix.utils.dp
import com.tanasi.streamflix.utils.viewModelsFactory

class SeasonMobileFragment : Fragment() {

    private var _binding: FragmentSeasonMobileBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<SeasonMobileFragmentArgs>()
    private val viewModel by viewModelsFactory { SeasonViewModel(args.seasonId) }

    private lateinit var database: AppDatabase

    private val appAdapter = AppAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSeasonMobileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = AppDatabase.getInstance(requireContext())

        initializeSeason()

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                SeasonViewModel.State.LoadingEpisodes -> binding.isLoading.apply {
                    root.visibility = View.VISIBLE
                    pbIsLoading.visibility = View.VISIBLE
                    gIsLoadingRetry.visibility = View.GONE
                }
                is SeasonViewModel.State.SuccessLoadingEpisodes -> {
                    displaySeason(state.episodes)
                    binding.isLoading.root.visibility = View.GONE
                }
                is SeasonViewModel.State.FailedLoadingEpisodes -> {
                    Toast.makeText(
                        requireContext(),
                        state.error.message ?: "",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.isLoading.apply {
                        pbIsLoading.visibility = View.GONE
                        gIsLoadingRetry.visibility = View.VISIBLE
                        btnIsLoadingRetry.setOnClickListener {
                            viewModel.getSeasonEpisodes(args.seasonId)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    fun refresh(episode: Episode) {
        appAdapter.notifyItemChanged(appAdapter.items.indexOf(episode))
    }


    private fun initializeSeason() {
        binding.tvSeasonTitle.text = args.seasonTitle

        binding.rvEpisodes.apply {
            adapter = appAdapter
            addItemDecoration(
                SpacingItemDecoration(20.dp(requireContext()))
            )
        }
    }

    private fun displaySeason(episodes: List<Episode>) {
        database.episodeDao().getByIds(episodes.map { it.id }).forEach { episodeDb ->
            episodes.find { it.id == episodeDb.id }
                ?.merge(episodeDb)
        }

        appAdapter.submitList(episodes.onEach { episode ->
            episode.tvShow = TvShow(
                id = args.tvShowId,
                title = args.tvShowTitle,
                poster = args.tvShowPoster,
                banner = args.tvShowBanner,
            )
            episode.season = args.seasonNumber.takeIf { it != 0 }?.let {
                Season(
                    id = args.seasonId,
                    number = args.seasonNumber,
                    title = args.seasonTitle,
                )
            }

            episode.itemType = AppAdapter.Type.EPISODE_MOBILE_ITEM
        })

        database.episodeDao().insertAll(episodes)

        val episodeIndex = episodes
            .sortedByDescending { it.watchHistory?.lastEngagementTimeUtcMillis }
            .indexOfFirst { it.watchHistory != null }.takeIf { it != -1 }
            ?: episodes.indexOfLast { it.isWatched }
                .takeIf { it != -1 && it + 1 < episodes.size }
                ?.let { it + 1 }

        if (episodeIndex != null) {
            val layoutManager = binding.rvEpisodes.layoutManager as? LinearLayoutManager
            layoutManager?.scrollToPositionWithOffset(
                episodeIndex,
                binding.rvEpisodes.height / 2 - 100.dp(requireContext())
            )
        }
    }
}