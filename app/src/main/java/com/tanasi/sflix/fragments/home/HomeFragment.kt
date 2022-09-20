package com.tanasi.sflix.fragments.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.tanasi.sflix.R
import com.tanasi.sflix.adapters.SflixAdapter
import com.tanasi.sflix.databinding.FragmentHomeBinding
import com.tanasi.sflix.models.Movie
import com.tanasi.sflix.models.Category
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
        if (_binding == null) {
            _binding = FragmentHomeBinding.inflate(inflater, container, false)
            viewModel.getHome()
        }
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
                is HomeViewModel.State.FailedLoading -> {
                    Toast.makeText(
                        requireContext(),
                        state.error.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }


    private fun displayHome() {
        binding.vgvHome.apply {
            val list = mutableListOf<SflixAdapter.Item>()

            list.add(Category(
                name = "Trending Movies",
                list = trendingMovies.onEach {
                    it.itemType = SflixAdapter.Type.MOVIE_ITEM
                }
            ).apply {
                itemSpacing = resources.getDimension(R.dimen.home_spacing).toInt()
            })

            list.add(Category(
                name = "Trending TV Shows",
                list = trendingTvShows.onEach {
                    it.itemType = SflixAdapter.Type.TV_SHOW_ITEM
                }
            ).apply {
                itemSpacing = resources.getDimension(R.dimen.home_spacing).toInt()
            })

            list.add(Category(
                name = "Latest Movies",
                list = latestMovies.onEach {
                    it.itemType = SflixAdapter.Type.MOVIE_ITEM
                }
            ).apply {
                itemSpacing = resources.getDimension(R.dimen.home_spacing).toInt()
            })

            list.add(Category(
                name = "Latest TV Shows",
                list = latestTvShows.onEach {
                    it.itemType = SflixAdapter.Type.TV_SHOW_ITEM
                }
            ).apply {
                itemSpacing = resources.getDimension(R.dimen.home_spacing).toInt()
            })

            adapter = SflixAdapter(list)
            setItemSpacing(resources.getDimension(R.dimen.home_spacing).toInt() * 2)
        }
    }
}