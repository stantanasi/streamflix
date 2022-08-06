package com.tanasi.sflix.fragments.movies

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.tanasi.sflix.R
import com.tanasi.sflix.adapters.SflixAdapter
import com.tanasi.sflix.databinding.FragmentMoviesBinding
import com.tanasi.sflix.models.Movie

class MoviesFragment : Fragment() {

    private var _binding: FragmentMoviesBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<MoviesViewModel>()

    private val movies = mutableListOf<Movie>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (_binding == null) {
            _binding = FragmentMoviesBinding.inflate(inflater, container, false)
            viewModel.getMovies()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                MoviesViewModel.State.Loading -> {}

                is MoviesViewModel.State.SuccessLoading -> {
                    movies.let {
                        it.clear()
                        it.addAll(state.movies)
                    }
                    displayMovies()
                }
                is MoviesViewModel.State.FailedLoading -> {
                    Toast.makeText(
                        requireContext(),
                        state.error.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }


    private fun displayMovies() {
        binding.vgvMovies.apply {
            adapter = SflixAdapter(movies.onEach {
                it.itemType = SflixAdapter.Type.MOVIE_MOVIES
            })
            setItemSpacing(requireContext().resources.getDimension(R.dimen.movies_spacing).toInt())
        }
    }
}