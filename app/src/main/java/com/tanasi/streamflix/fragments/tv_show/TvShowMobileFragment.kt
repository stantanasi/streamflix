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
import com.tanasi.streamflix.databinding.FragmentTvShowMobileBinding
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.ui.SpacingItemDecoration
import com.tanasi.streamflix.utils.dp
import com.tanasi.streamflix.utils.viewModelsFactory
import kotlinx.coroutines.launch

class TvShowMobileFragment : Fragment() {

    private var _binding: FragmentTvShowMobileBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<TvShowMobileFragmentArgs>()
    private val database by lazy { AppDatabase.getInstance(requireContext()) }
    private val viewModel by viewModelsFactory { TvShowViewModel(args.id, database) }

    private val appAdapter = AppAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTvShowMobileBinding.inflate(inflater, container, false)
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
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        appAdapter.onSaveInstanceState(binding.rvTvShow)
        _binding = null
    }


    private fun initializeTvShow() {
        binding.rvTvShow.apply {
            adapter = appAdapter.apply {
                stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }
            addItemDecoration(
                SpacingItemDecoration(20.dp(requireContext()))
            )
        }
    }

    private fun displayTvShow(tvShow: TvShow) {
        Glide.with(requireContext())
            .load(tvShow.banner)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.ivTvShowBanner)

        appAdapter.submitList(listOfNotNull(
            tvShow.apply { itemType = AppAdapter.Type.TV_SHOW_MOBILE },

            tvShow.takeIf { it.seasons.isNotEmpty() }
                ?.copy()
                ?.apply { itemType = AppAdapter.Type.TV_SHOW_SEASONS_MOBILE },

            tvShow.takeIf { it.cast.isNotEmpty() }
                ?.copy()
                ?.apply { itemType = AppAdapter.Type.TV_SHOW_CAST_MOBILE },

            tvShow.takeIf { it.recommendations.isNotEmpty() }
                ?.copy()
                ?.apply { itemType = AppAdapter.Type.TV_SHOW_RECOMMENDATIONS_MOBILE },
        ))
    }
}