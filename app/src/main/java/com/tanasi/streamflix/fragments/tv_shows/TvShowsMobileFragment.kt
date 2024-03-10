package com.tanasi.streamflix.fragments.tv_shows

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.databinding.FragmentTvShowsMobileBinding
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.ui.SpacingItemDecoration
import com.tanasi.streamflix.utils.dp

class TvShowsMobileFragment : Fragment() {

    private var _binding: FragmentTvShowsMobileBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<TvShowsViewModel>()

    private val appAdapter = AppAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTvShowsMobileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeTvShows()

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                TvShowsViewModel.State.Loading -> binding.isLoading.apply {
                    root.visibility = View.VISIBLE
                    pbIsLoading.visibility = View.VISIBLE
                    gIsLoadingRetry.visibility = View.GONE
                }
                TvShowsViewModel.State.LoadingMore -> appAdapter.isLoading = true
                is TvShowsViewModel.State.SuccessLoading -> {
                    displayTvShows(state.tvShows, state.hasMore)
                    appAdapter.isLoading = false
                    binding.isLoading.root.visibility = View.GONE
                }
                is TvShowsViewModel.State.FailedLoading -> {
                    Toast.makeText(
                        requireContext(),
                        state.error.message ?: "",
                        Toast.LENGTH_SHORT
                    ).show()
                    if (appAdapter.isLoading) {
                        appAdapter.isLoading = false
                    } else {
                        binding.isLoading.apply {
                            pbIsLoading.visibility = View.GONE
                            gIsLoadingRetry.visibility = View.VISIBLE
                            btnIsLoadingRetry.setOnClickListener {
                                viewModel.getTvShows()
                            }
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


    private fun initializeTvShows() {
        binding.rvTvShows.apply {
            adapter = appAdapter
            addItemDecoration(
                SpacingItemDecoration(10.dp(requireContext()))
            )
        }
    }

    private fun displayTvShows(tvShows: List<TvShow>, hasMore: Boolean) {
        appAdapter.submitList(tvShows.onEach {
            it.itemType = AppAdapter.Type.TV_SHOW_GRID_MOBILE_ITEM
        })

        if (hasMore) {
            appAdapter.setOnLoadMoreListener { viewModel.loadMoreTvShows() }
        } else {
            appAdapter.setOnLoadMoreListener(null)
        }
    }
}