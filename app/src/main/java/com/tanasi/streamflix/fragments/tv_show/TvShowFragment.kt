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
                TvShowViewModel.State.Loading -> binding.isLoading.root.visibility = View.VISIBLE
                is TvShowViewModel.State.SuccessLoading -> {
                    val episodes = database.episodeDao().getEpisodesByTvShowId(state.tvShow.id)
                    if (episodes.isEmpty()) {
                        viewModel.getFirstSeason(state.tvShow)
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
                }
            }
        }

        viewModel.seasonState.observe(viewLifecycleOwner) { state ->
            when (state) {
                TvShowViewModel.SeasonState.Loading -> {}
                is TvShowViewModel.SeasonState.SuccessLoading -> {
                    val episodes = database.episodeDao().getEpisodesByTvShowId(state.tvShow.id)
                    if (episodes.isEmpty()) {
                        state.episodes.onEach { episode ->
                            episode.tvShow = state.tvShow
                            episode.season = state.season.takeIf { it.number != 0 }
                        }
                        database.episodeDao().insertAll(state.episodes)
                        appAdapter.notifyItemChanged(0)
                    }
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