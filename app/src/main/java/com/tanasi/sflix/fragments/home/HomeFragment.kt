package com.tanasi.sflix.fragments.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.*
import androidx.navigation.fragment.findNavController
import com.tanasi.sflix.databinding.FragmentHomeBinding
import com.tanasi.sflix.models.Movie
import com.tanasi.sflix.models.TvShow
import com.tanasi.sflix.presenters.MoviePresenter
import com.tanasi.sflix.presenters.TvShowPresenter

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<HomeViewModel>()

    private val trendingMovies = mutableListOf<Movie>()
    private val trendingTvShows = mutableListOf<TvShow>()

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
                    displayHome()
                }
            }
        }

        viewModel.fetchHome()
    }


    private fun displayHome() {
        val rowsFragment = RowsSupportFragment()

        parentFragmentManager
            .beginTransaction()
            .replace(binding.flHomeRows.id, rowsFragment)
            .commit()

        rowsFragment.onItemViewClickedListener =
            OnItemViewClickedListener { _, item, _, _ ->
                when (item) {
                    is Movie -> findNavController().navigate(
                        HomeFragmentDirections.actionHomeToMovie(
                            id = item.id
                        )
                    )
                    is TvShow -> {}
                }
            }

        rowsFragment.adapter = ArrayObjectAdapter(ListRowPresenter()).apply {
            add(ListRow(
                HeaderItem("Trending Movies"),
                ArrayObjectAdapter(MoviePresenter()).apply {
                    clear()
                    addAll(0, trendingMovies)
                }
            ))

            add(ListRow(
                HeaderItem("Trending TV Shows"),
                ArrayObjectAdapter(TvShowPresenter()).apply {
                    clear()
                    addAll(0, trendingTvShows)
                }
            ))
        }
    }
}