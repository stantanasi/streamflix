package com.tanasi.streamflix.fragments.movie

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.database.AppDatabase
import com.tanasi.streamflix.databinding.FragmentMovieTvBinding
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.utils.viewModelsFactory
import kotlinx.coroutines.launch

class MovieTvFragment : Fragment() {

    private var _binding: FragmentMovieTvBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<MovieTvFragmentArgs>()
    private val database by lazy { AppDatabase.getInstance(requireContext()) }
    private val viewModel by viewModelsFactory { MovieViewModel(args.id, database) }

    private val appAdapter = AppAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMovieTvBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeMovie()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED).collect { state ->
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
                            btnIsLoadingRetry.requestFocus()
                        }
                    }
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
            adapter = appAdapter.apply {
                stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }
            setItemSpacing(80)
        }
    }

    private fun displayMovie(movie: Movie) {
        Glide.with(requireContext())
            .load(movie.banner)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.ivMovieBanner)

        appAdapter.submitList(listOfNotNull(
            movie.apply { itemType = AppAdapter.Type.MOVIE_TV },

            movie.takeIf { it.cast.isNotEmpty() }
                ?.copy()
                ?.apply { itemType = AppAdapter.Type.MOVIE_CAST_TV },

            movie.takeIf { it.recommendations.isNotEmpty() }
                ?.copy()
                ?.apply { itemType = AppAdapter.Type.MOVIE_RECOMMENDATIONS_TV },
        ))
    }
}