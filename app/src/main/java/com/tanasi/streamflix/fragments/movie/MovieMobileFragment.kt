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
import com.tanasi.streamflix.database.AppDatabase
import com.tanasi.streamflix.databinding.FragmentMovieMobileBinding
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.ui.SpacingItemDecoration
import com.tanasi.streamflix.utils.dp
import com.tanasi.streamflix.utils.viewModelsFactory

class MovieMobileFragment : Fragment() {

    private var _binding: FragmentMovieMobileBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<MovieMobileFragmentArgs>()
    private val viewModel by viewModelsFactory { MovieViewModel(args.id) }

    private lateinit var database: AppDatabase

    private val appAdapter = AppAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMovieMobileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = AppDatabase.getInstance(requireContext())

        initializeMovie()

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                MovieViewModel.State.Loading -> binding.isLoading.apply {
                    root.visibility = View.VISIBLE
                    pbIsLoading.visibility = View.VISIBLE
                    gIsLoadingRetry.visibility = View.GONE
                }
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
                    binding.isLoading.apply {
                        pbIsLoading.visibility = View.GONE
                        gIsLoadingRetry.visibility = View.VISIBLE
                        btnIsLoadingRetry.setOnClickListener {
                            viewModel.getMovie(args.id)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        appAdapter.onSaveInstanceState(binding.rvMovie)
        _binding = null
    }


    private fun initializeMovie() {
        binding.rvMovie.apply {
            adapter = appAdapter
            addItemDecoration(
                SpacingItemDecoration(20.dp(requireContext()))
            )
        }
    }

    private fun displayMovie(movie: Movie) {
        database.movieDao().getById(movie.id)?.let {
            movie.isFavorite = it.isFavorite
            movie.isWatched = it.isWatched
        }
        database.movieDao().insert(movie)

        Glide.with(requireContext())
            .load(movie.banner)
            .into(binding.ivMovieBanner)

        appAdapter.submitList(listOfNotNull(
            movie.apply { itemType = AppAdapter.Type.MOVIE_MOBILE },

            movie.takeIf { it.cast.isNotEmpty() }
                ?.clone()
                ?.apply { itemType = AppAdapter.Type.MOVIE_CASTS_MOBILE },

            movie.takeIf { it.recommendations.isNotEmpty() }
                ?.clone()
                ?.apply { itemType = AppAdapter.Type.MOVIE_RECOMMENDATIONS_MOBILE },
        ))
    }
}