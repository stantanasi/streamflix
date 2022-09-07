package com.tanasi.sflix.fragments.season

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.tanasi.sflix.adapters.SflixAdapter
import com.tanasi.sflix.databinding.FragmentSeasonBinding

class SeasonFragment : Fragment() {

    private var _binding: FragmentSeasonBinding? = null
    private val binding get() = _binding!!

    val viewModel by viewModels<SeasonViewModel>()
    private val args by navArgs<SeasonFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (_binding == null) {
            _binding = FragmentSeasonBinding.inflate(inflater, container, false)
            viewModel.getSeasonEpisodes(args.seasonId)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvSeasonTitle.text = args.seasonTitle

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                SeasonViewModel.State.LoadingEpisodes -> {}
                is SeasonViewModel.State.SuccessLoadingEpisodes -> {
                    binding.vgvEpisodes.apply {
                        adapter = SflixAdapter(state.episodes)
                        setItemSpacing(60)
                    }
                }
                is SeasonViewModel.State.FailedLoadingEpisodes -> {
                    Toast.makeText(
                        requireContext(),
                        state.error.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}