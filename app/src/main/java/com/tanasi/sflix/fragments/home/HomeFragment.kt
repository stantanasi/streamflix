package com.tanasi.sflix.fragments.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import com.tanasi.sflix.R
import com.tanasi.sflix.models.Movie
import com.tanasi.sflix.models.TvShow
import com.tanasi.sflix.presenters.MoviePresenter
import com.tanasi.sflix.presenters.TvShowPresenter

class HomeFragment : BrowseSupportFragment() {

    private val viewModel by viewModels<HomeViewModel>()

    private val trendingMovies = mutableListOf<Movie>()
    private val trendingTvShows = mutableListOf<TvShow>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        title = getString(R.string.app_name)

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
        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())

        rowsAdapter.add(ListRow(
            HeaderItem("Trending Movies"),
            ArrayObjectAdapter(MoviePresenter()).apply {
                clear()
                addAll(0, trendingMovies)
            }
        ))

        rowsAdapter.add(ListRow(
            HeaderItem("Trending TV Shows"),
            ArrayObjectAdapter(TvShowPresenter()).apply {
                clear()
                addAll(0, trendingTvShows)
            }
        ))

        adapter = rowsAdapter
    }
}