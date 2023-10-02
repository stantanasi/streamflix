package com.tanasi.streamflix.fragments.season

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.tanasi.streamflix.R
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.databinding.FragmentSeasonBinding
import com.tanasi.streamflix.models.Episode
import com.tanasi.streamflix.models.Season
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.utils.viewModelsFactory

class SeasonFragment : Fragment() {

    private var _binding: FragmentSeasonBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<SeasonFragmentArgs>()
    private val viewModel by viewModelsFactory { SeasonViewModel(args.seasonId) }

    private val appAdapter = AppAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSeasonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeSeason()

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                SeasonViewModel.State.LoadingEpisodes -> binding.isLoading.root.visibility = View.VISIBLE
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
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun initializeSeason() {
        binding.tvSeasonTitle.text = args.seasonTitle

        binding.vgvEpisodes.apply {
            adapter = appAdapter
            setItemSpacing(resources.getDimension(R.dimen.season_episodes_spacing).toInt())
        }
    }

    private fun displaySeason(episodes: List<Episode>) {
        appAdapter.items.apply {
            clear()
            addAll(episodes.onEach { episode ->
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
            })
        }
        appAdapter.notifyDataSetChanged()
    }
}