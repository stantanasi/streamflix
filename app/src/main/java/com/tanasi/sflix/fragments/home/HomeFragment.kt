package com.tanasi.sflix.fragments.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.tanasi.sflix.adapters.SflixAdapter
import com.tanasi.sflix.databinding.FragmentHomeBinding
import com.tanasi.sflix.models.Movie
import com.tanasi.sflix.models.TvShow

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<HomeViewModel>()

    private val trendingMovies = mutableListOf<Movie>()
    private val trendingTvShows = mutableListOf<TvShow>()
    private val latestMovies = mutableListOf<Movie>()
    private val latestTvShows = mutableListOf<TvShow>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                HomeViewModel.State.Loading -> {}
                is HomeViewModel.State.SuccessLoading -> {
                    trendingMovies.apply {
                        clear()
                        addAll(state.trendingMovies)
                    }
                    trendingTvShows.apply {
                        clear()
                        addAll(state.trendingTvShows)
                    }
                    latestMovies.apply {
                        clear()
                        addAll(state.latestMovies)
                    }
                    latestTvShows.apply {
                        clear()
                        addAll(state.latestTvShows)
                    }
                    displayHome()
                }
            }
        }

        viewModel.fetchHome()
    }


    private fun displayHome() {
        binding.hgvTrendingMovies.apply {
            setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
            adapter = SflixAdapter(trendingMovies.map {
                it.apply { itemType = SflixAdapter.Type.MOVIE }
            })
        }

        binding.hgvTrendingTvShows.apply {
            setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
            adapter = SflixAdapter(trendingTvShows.map {
                it.apply { itemType = SflixAdapter.Type.TV_SHOW }
            })
        }

        binding.hgvLatestMovies.apply {
            setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
            adapter = SflixAdapter(latestMovies.map {
                it.apply { itemType = SflixAdapter.Type.MOVIE }
            })
        }

        binding.hgvLatestTvShows.apply {
            setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
            adapter = SflixAdapter(latestTvShows.map {
                it.apply { itemType = SflixAdapter.Type.TV_SHOW }
            })
        }
    }
}