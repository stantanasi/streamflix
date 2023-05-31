package com.tanasi.sflix.fragments.movies

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.tanasi.sflix.R
import com.tanasi.sflix.adapters.AppAdapter
import com.tanasi.sflix.databinding.FragmentMoviesBinding
import com.tanasi.sflix.models.Movie

class MoviesFragment : Fragment() {

    private var _binding: FragmentMoviesBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<MoviesViewModel>()

    private val appAdapter = AppAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoviesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeMovies()

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                MoviesViewModel.State.Loading -> binding.isLoading.root.visibility = View.VISIBLE
                is MoviesViewModel.State.SuccessLoading -> {
                    displayMovies(state.movies)
                    binding.isLoading.root.visibility = View.GONE
                }
                is MoviesViewModel.State.FailedLoading -> {
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


    private fun initializeMovies() {
        binding.vgvMovies.apply {
            adapter = appAdapter
            setItemSpacing(requireContext().resources.getDimension(R.dimen.movies_spacing).toInt())
        }
    }

    private fun displayMovies(movies: List<Movie>) {
        appAdapter.items.apply {
            clear()
            addAll(movies.onEach {
                it.itemType = AppAdapter.Type.MOVIE_GRID_ITEM
            })
        }
        appAdapter.notifyDataSetChanged()
    }
}