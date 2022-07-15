package com.tanasi.sflix.fragments.seasons

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.tanasi.sflix.adapters.SflixAdapter
import com.tanasi.sflix.databinding.FragmentSeasonsBinding

class SeasonsFragment : Fragment() {

    private var _binding: FragmentSeasonsBinding? = null
    private val binding get() = _binding!!

    val viewModel by viewModels<SeasonsViewModel>()
    private val args by navArgs<SeasonsFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSeasonsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Glide.with(requireContext())
            .load(args.tvShowBanner)
            .centerCrop()
            .into(binding.ivTvShowBanner)

        binding.tvTvShowTitle.text = args.tvShowTitle

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                SeasonsViewModel.State.LoadingSeasons -> {}
                is SeasonsViewModel.State.SuccessLoadingSeasons -> {
                    binding.vgvSeasons.apply {
                        adapter = SflixAdapter(state.seasons.onEach {
                            it.itemType = SflixAdapter.Type.SEASON
                        })
                        setItemSpacing(20)
                    }
                    viewModel.getSeasonEpisodes(state.seasons.firstOrNull()?.id ?: "")
                }
                is SeasonsViewModel.State.FailedLoadingSeasons -> {
                    Toast.makeText(
                        requireContext(),
                        state.error.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                SeasonsViewModel.State.LoadingEpisodes -> {}
                is SeasonsViewModel.State.SuccessLoadingEpisodes -> {
                    binding.vgvEpisodes.apply {
                        adapter = SflixAdapter(state.episodes.onEach {
                            it.itemType = SflixAdapter.Type.EPISODE
                        })
                        setItemSpacing(20)
                    }
                }
                is SeasonsViewModel.State.FailedLoadingEpisodes -> {
                    Toast.makeText(
                        requireContext(),
                        state.error.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        viewModel.getSeasons(args.tvShowId)
    }
}