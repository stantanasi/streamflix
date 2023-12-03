package com.tanasi.streamflix.fragments.movie

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.databinding.FragmentMovieBinding
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.utils.viewModelsFactory

class MovieFragment : Fragment() {

    private var _binding: FragmentMovieBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<MovieFragmentArgs>()
    private val viewModel by viewModelsFactory { MovieViewModel(args.id) }

    private val appAdapter = AppAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMovieBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeMovie()

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                MovieViewModel.State.Loading -> binding.isLoading.root.visibility = View.VISIBLE
                is MovieViewModel.State.SuccessLoading -> {
                    displayMovie(state.movie)
                    binding.isLoading.root.visibility = View.GONE
                }
                is MovieViewModel.State.FailedLoading -> {
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
        appAdapter.onSaveInstanceState(binding.vgvMovie)
        _binding = null
    }


    private fun initializeMovie() {
        binding.vgvMovie.apply {
            adapter = appAdapter
            setItemSpacing(80)
        }
    }

    private fun displayMovie(movie: Movie) {
        Glide.with(requireContext())
            .load(movie.banner)
            .into(binding.ivMovieBanner)

        appAdapter.items.apply {
            clear()
            add(movie.apply { itemType = AppAdapter.Type.MOVIE })
            if (movie.cast.isNotEmpty())
                add(movie.clone().apply { itemType = AppAdapter.Type.MOVIE_CASTS })
            if (movie.recommendations.isNotEmpty())
                add(movie.clone().apply { itemType = AppAdapter.Type.MOVIE_RECOMMENDATIONS })
        }
        appAdapter.notifyDataSetChanged()
    }
}