package com.tanasi.sflix.fragments.movie

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.tanasi.sflix.adapters.SflixAdapter
import com.tanasi.sflix.databinding.FragmentMovieBinding
import com.tanasi.sflix.models.Movie
import com.tanasi.sflix.utils.format

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
        _binding = FragmentMovieBinding.inflate(inflater, container, false)
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
            }
        }

        viewModel.fetchMovie(args.id)
    }


    private fun displayMovie() {
        Glide.with(requireContext())
            .load(movie.banner)
            .into(binding.ivMovieBanner)

        binding.vgvMovie.apply {
            adapter = SflixAdapter(mutableListOf<SflixAdapter.Item>().also {
                it.add(movie.apply { itemType = SflixAdapter.Type.MOVIE_HEADER })
            })
        }
    }
}