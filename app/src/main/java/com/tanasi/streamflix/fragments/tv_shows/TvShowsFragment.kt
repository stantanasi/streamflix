package com.tanasi.streamflix.fragments.tv_shows

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.tanasi.streamflix.R
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.databinding.FragmentTvShowsBinding
import com.tanasi.streamflix.models.TvShow

class TvShowsFragment : Fragment() {

    private var _binding: FragmentTvShowsBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<TvShowsViewModel>()

    private val appAdapter = AppAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTvShowsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeTvShows()

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                TvShowsViewModel.State.Loading -> binding.isLoading.root.visibility = View.VISIBLE
                is TvShowsViewModel.State.SuccessLoading -> {
                    displayTvShows(state.tvShows)
                    binding.isLoading.root.visibility = View.GONE
                }
                is TvShowsViewModel.State.FailedLoading -> {
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


    private fun initializeTvShows() {
        binding.vgvTvShows.apply {
            adapter = appAdapter
            setItemSpacing(
                requireContext().resources.getDimension(R.dimen.tv_shows_spacing).toInt()
            )
        }
    }

    private fun displayTvShows(tvShows: List<TvShow>) {
        appAdapter.items.apply {
            clear()
            addAll(tvShows.onEach {
                it.itemType = AppAdapter.Type.TV_SHOW_GRID_ITEM
            })
        }
        appAdapter.notifyDataSetChanged()
    }
}