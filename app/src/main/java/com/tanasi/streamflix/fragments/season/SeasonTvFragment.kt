package com.tanasi.streamflix.fragments.season

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tanasi.streamflix.R
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.database.AppDatabase
import com.tanasi.streamflix.databinding.FragmentSeasonTvBinding
import com.tanasi.streamflix.models.Episode
import com.tanasi.streamflix.utils.dp
import com.tanasi.streamflix.utils.viewModelsFactory
import kotlinx.coroutines.launch

class SeasonTvFragment : Fragment() {

    private var _binding: FragmentSeasonTvBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<SeasonTvFragmentArgs>()
    private val database by lazy { AppDatabase.getInstance(requireContext()) }
    private val viewModel by viewModelsFactory {
        SeasonViewModel(
            args.seasonId,
            args.tvShowId,
            database,
        )
    }

    private val appAdapter = AppAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSeasonTvBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeSeason()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED).collect { state ->
                when (state) {
                    SeasonViewModel.State.LoadingEpisodes -> binding.isLoading.apply {
                        root.visibility = View.VISIBLE
                        pbIsLoading.visibility = View.VISIBLE
                        gIsLoadingRetry.visibility = View.GONE
                    }

                    is SeasonViewModel.State.SuccessLoadingEpisodes -> {
                        displaySeason(state.episodes)
                        binding.isLoading.root.visibility = View.GONE
                    }

                    is SeasonViewModel.State.FailedLoadingEpisodes -> {
                        Toast.makeText(
                            requireContext(),
                            state.error.message ?: "",
                            Toast.LENGTH_SHORT
                        ).show()
                        binding.isLoading.apply {
                            pbIsLoading.visibility = View.GONE
                            gIsLoadingRetry.visibility = View.VISIBLE
                            btnIsLoadingRetry.setOnClickListener {
                                viewModel.getSeasonEpisodes(args.seasonId)
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
        _binding = null
    }


    private fun initializeSeason() {
        binding.tvSeasonTitle.text = args.seasonTitle

        binding.hgvEpisodes.apply {
            adapter = appAdapter.apply {
                stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }
            setItemSpacing(resources.getDimension(R.dimen.season_episodes_spacing).toInt())
        }
    }

    private var focusedEpisodeIndex: Int? = null

    private fun displaySeason(episodes: List<Episode>) {
        val preparedEpisodes = episodes.onEach { episode ->
            episode.itemType = AppAdapter.Type.EPISODE_TV_ITEM
        }

        val lastWatchedIndex = episodes
            .filter { it.watchHistory != null }
            .sortedByDescending { it.watchHistory?.lastEngagementTimeUtcMillis }
            .firstOrNull()
            ?.let { episodes.indexOf(it) }
            ?: episodes.indexOfLast { it.isWatched }

        appAdapter.submitList(preparedEpisodes)

        if (focusedEpisodeIndex == null) {
            val scrollIndex = when {
                lastWatchedIndex == -1 -> 0
                lastWatchedIndex < episodes.lastIndex -> lastWatchedIndex + 1
                else -> lastWatchedIndex
            }
            binding.hgvEpisodes.scrollAndFocus(scrollIndex)
            focusedEpisodeIndex = scrollIndex
        }
    }

    private fun RecyclerView.scrollAndFocus(position: Int) {
        scrollToPosition(position)
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                findViewHolderForAdapterPosition(position)?.itemView?.requestFocus()
            }
        })
    }



}