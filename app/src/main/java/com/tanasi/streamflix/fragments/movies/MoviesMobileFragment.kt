package com.tanasi.streamflix.fragments.movies

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.databinding.FragmentMoviesMobileBinding
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.ui.SpacingItemDecoration
import com.tanasi.streamflix.utils.dp

class MoviesMobileFragment : Fragment() {

    private var _binding: FragmentMoviesMobileBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<MoviesViewModel>()

    private val appAdapter = AppAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoviesMobileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeMovies()

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                MoviesViewModel.State.Loading -> binding.isLoading.apply {
                    root.visibility = View.VISIBLE
                    pbIsLoading.visibility = View.VISIBLE
                    gIsLoadingRetry.visibility = View.GONE
                }
                MoviesViewModel.State.LoadingMore -> appAdapter.isLoading = true
                is MoviesViewModel.State.SuccessLoading -> {
                    displayMovies(state.movies, state.hasMore)
                    appAdapter.isLoading = false
                    binding.isLoading.root.visibility = View.GONE
                }
                is MoviesViewModel.State.FailedLoading -> {
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
                                viewModel.getMovies()
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


    private fun initializeMovies() {
        binding.rvMovies.apply {
            adapter = appAdapter
            addItemDecoration(
                SpacingItemDecoration(10.dp(requireContext()))
            )
        }
    }

    private fun displayMovies(movies: List<Movie>, hasMore: Boolean) {
        appAdapter.submitList(movies.onEach {
            it.itemType = AppAdapter.Type.MOVIE_GRID_MOBILE_ITEM
        })

        if (hasMore) {
            appAdapter.setOnLoadMoreListener { viewModel.loadMoreMovies() }
        } else {
            appAdapter.setOnLoadMoreListener(null)
        }
    }
}