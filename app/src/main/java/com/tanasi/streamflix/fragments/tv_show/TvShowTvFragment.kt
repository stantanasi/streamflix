package com.tanasi.streamflix.fragments.tv_show

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
import com.tanasi.streamflix.databinding.FragmentTvShowTvBinding
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.utils.viewModelsFactory
import kotlinx.coroutines.launch

class TvShowTvFragment : Fragment() {

    private var _binding: FragmentTvShowTvBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<TvShowTvFragmentArgs>()
    private val database by lazy { AppDatabase.getInstance(requireContext()) }
    private val viewModel by viewModelsFactory { TvShowViewModel(args.id, database) }

    private val appAdapter = AppAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTvShowTvBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeTvShow()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED).collect { state ->
                when (state) {
                    TvShowViewModel.State.Loading -> binding.isLoading.apply {
                        root.visibility = View.VISIBLE
                        pbIsLoading.visibility = View.VISIBLE
                        gIsLoadingRetry.visibility = View.GONE
                    }
                    is TvShowViewModel.State.SuccessLoading -> {
                        displayTvShow(state.tvShow)
                        binding.isLoading.root.visibility = View.GONE
                    }
                    is TvShowViewModel.State.FailedLoading -> {
                        Toast.makeText(
                            requireContext(),
                            state.error.message ?: "",
                            Toast.LENGTH_SHORT
                        ).show()
                        binding.isLoading.apply {
                            pbIsLoading.visibility = View.GONE
                            gIsLoadingRetry.visibility = View.VISIBLE
                            btnIsLoadingRetry.setOnClickListener {
                                viewModel.getTvShow(args.id)
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
        appAdapter.onSaveInstanceState(binding.vgvTvShow)
        _binding = null
    }


    private fun initializeTvShow() {
        binding.vgvTvShow.apply {
            adapter = appAdapter.apply {
                stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }
            setItemSpacing(80)
        }
    }

    private fun displayTvShow(tvShow: TvShow) {
        Glide.with(requireContext())
            .load(tvShow.banner)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.ivTvShowBanner)

        appAdapter.submitList(listOfNotNull(
            tvShow.apply { itemType = AppAdapter.Type.TV_SHOW_TV },

            tvShow.takeIf { it.seasons.isNotEmpty() }
                ?.copy()
                ?.apply { itemType = AppAdapter.Type.TV_SHOW_SEASONS_TV },

            tvShow.takeIf { it.cast.isNotEmpty() }
                ?.copy()
                ?.apply { itemType = AppAdapter.Type.TV_SHOW_CAST_TV },

            tvShow.takeIf { it.recommendations.isNotEmpty() }
                ?.copy()
                ?.apply { itemType = AppAdapter.Type.TV_SHOW_RECOMMENDATIONS_TV },
        ))
    }
}