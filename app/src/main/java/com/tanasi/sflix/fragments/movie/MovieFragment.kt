package com.tanasi.sflix.fragments.movie

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.tanasi.sflix.adapters.SflixAdapter
import com.tanasi.sflix.databinding.FragmentMovieBinding
import com.tanasi.sflix.models.Movie

class MovieFragment : Fragment() {

    private var _binding: FragmentMovieBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<MovieFragmentArgs>()
    private val viewModel by viewModels<MovieViewModel>()

    private val sflixAdapter = SflixAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMovieBinding.inflate(inflater, container, false)
        viewModel.getMovieById(args.id)
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
                        state.error.message,
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


    private fun initializeMovie() {
        binding.vgvMovie.apply {
            adapter = sflixAdapter
            setItemSpacing(80)
        }
    }

    private fun displayMovie(movie: Movie) {
        Glide.with(requireContext())
            .load(movie.banner)
            .into(binding.ivMovieBanner)

        sflixAdapter.items.apply {
            clear()
            add(movie.apply { itemType = SflixAdapter.Type.MOVIE })
            if (movie.cast.isNotEmpty())
                add(movie.clone().apply { itemType = SflixAdapter.Type.MOVIE_CASTS })
            if (movie.recommendations.isNotEmpty())
                add(movie.clone().apply { itemType = SflixAdapter.Type.MOVIE_RECOMMENDATIONS })
        }
        sflixAdapter.notifyDataSetChanged()
    }
}