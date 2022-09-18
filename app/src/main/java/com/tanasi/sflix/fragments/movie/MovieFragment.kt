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

    private lateinit var movie: Movie

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (_binding == null) {
            _binding = FragmentMovieBinding.inflate(inflater, container, false)
            viewModel.getMovieById(args.id)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                MovieViewModel.State.Loading -> {}

                is MovieViewModel.State.SuccessLoading -> {
                    movie = state.movie
                    displayMovie()
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


    private fun displayMovie() {
        Glide.with(requireContext())
            .load(movie.banner)
            .into(binding.ivMovieBanner)

        binding.vgvMovie.apply {
            adapter = SflixAdapter(mutableListOf<SflixAdapter.Item>().also {
                it.add(movie.apply { itemType = SflixAdapter.Type.MOVIE })
                if (movie.cast.isNotEmpty())
                    it.add(movie.clone().apply { itemType = SflixAdapter.Type.MOVIE_CASTS })
                if (movie.recommendations.isNotEmpty())
                    it.add(movie.clone().apply { itemType = SflixAdapter.Type.MOVIE_RECOMMENDATIONS })
            })
            setItemSpacing(80)
        }
    }
}