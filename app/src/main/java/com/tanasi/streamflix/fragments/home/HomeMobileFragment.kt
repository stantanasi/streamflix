package com.tanasi.streamflix.fragments.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import com.tanasi.streamflix.NavMainGraphDirections
import com.tanasi.streamflix.R
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.database.AppDatabase
import com.tanasi.streamflix.databinding.FragmentHomeMobileBinding
import com.tanasi.streamflix.models.Category
import com.tanasi.streamflix.models.Episode
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.ui.SpacingItemDecoration
import com.tanasi.streamflix.utils.UserPreferences
import com.tanasi.streamflix.utils.dp

class HomeMobileFragment : Fragment() {

    private var _binding: FragmentHomeMobileBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<HomeViewModel>()

    private lateinit var database: AppDatabase

    private val appAdapter = AppAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeMobileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = AppDatabase.getInstance(requireContext())

        initializeHome()

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                HomeViewModel.State.Loading -> binding.isLoading.apply {
                    root.visibility = View.VISIBLE
                    pbIsLoading.visibility = View.VISIBLE
                    gIsLoadingRetry.visibility = View.GONE
                }
                is HomeViewModel.State.SuccessLoading -> {
                    displayHome(state.categories)
                    binding.isLoading.root.visibility = View.GONE
                }
                is HomeViewModel.State.FailedLoading -> {
                    Toast.makeText(
                        requireContext(),
                        state.error.message ?: "",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.isLoading.apply {
                        pbIsLoading.visibility = View.GONE
                        gIsLoadingRetry.visibility = View.VISIBLE
                        btnIsLoadingRetry.setOnClickListener {
                            viewModel.getHome()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        appAdapter.onSaveInstanceState(binding.rvHome)
        _binding = null
    }


    fun refresh() {
        appAdapter.onSaveInstanceState(binding.rvHome)
        when (val state = viewModel.state.value) {
            is HomeViewModel.State.SuccessLoading -> displayHome(state.categories)
            else -> {}
        }
    }

    private fun initializeHome() {
        binding.rvHome.apply {
            adapter = appAdapter
            addItemDecoration(
                SpacingItemDecoration(20.dp(requireContext()))
            )
        }

        binding.ivProviderLogo.apply {
            Glide.with(context)
                .load(UserPreferences.currentProvider!!.logo)
                .fitCenter()
                .into(this)

            setOnClickListener {
                findNavController().navigate(NavMainGraphDirections.actionGlobalProviders())
            }
        }
    }

    private fun displayHome(categories: List<Category>) {
        appAdapter.submitList(listOfNotNull(
            categories
                .find { it.name == Category.FEATURED }
                ?.also { it.itemType = AppAdapter.Type.CATEGORY_MOBILE_SWIPER },

            Category(
                name = getString(R.string.home_continue_watching),
                list = listOf(
                    database.movieDao().getWatchingMovies(),
                    database.episodeDao().getWatchingEpisodes().onEach { episode ->
                        episode.tvShow = episode.tvShow?.let { database.tvShowDao().getById(it.id) }
                        episode.season = episode.season?.let { database.seasonDao().getById(it.id) }
                    },
                ).flatten().sortedByDescending { it.watchHistory?.lastEngagementTimeUtcMillis }
            ).takeIf { it.list.isNotEmpty() }?.also {
                it.list.onEach { show ->
                    when (show) {
                        is Movie -> show.itemType = AppAdapter.Type.MOVIE_MOBILE_ITEM
                        is Episode -> show.itemType = AppAdapter.Type.EPISODE_CONTINUE_WATCHING_MOBILE_ITEM
                    }
                }
                it.itemSpacing = resources.getDimension(R.dimen.home_spacing).toInt()
                it.itemType = AppAdapter.Type.CATEGORY_MOBILE_ITEM
            },

            Category(
                name = getString(R.string.home_favorite_movies),
                list = database.movieDao().getFavorites()
                    .reversed(),
            ).takeIf { it.list.isNotEmpty() }?.also {
                it.list.onEach { show ->
                    when (show) {
                        is Movie -> show.itemType = AppAdapter.Type.MOVIE_MOBILE_ITEM
                        is TvShow -> show.itemType = AppAdapter.Type.TV_SHOW_MOBILE_ITEM
                    }
                }
                it.itemSpacing = resources.getDimension(R.dimen.home_spacing).toInt()
                it.itemType = AppAdapter.Type.CATEGORY_MOBILE_ITEM
            },

            Category(
                name = getString(R.string.home_favorite_tv_shows),
                list = database.tvShowDao().getFavorites()
                    .reversed(),
            ).takeIf { it.list.isNotEmpty() }?.also {
                it.list.onEach { show ->
                    when (show) {
                        is Movie -> show.itemType = AppAdapter.Type.MOVIE_MOBILE_ITEM
                        is TvShow -> show.itemType = AppAdapter.Type.TV_SHOW_MOBILE_ITEM
                    }
                }
                it.itemSpacing = resources.getDimension(R.dimen.home_spacing).toInt()
                it.itemType = AppAdapter.Type.CATEGORY_MOBILE_ITEM
            },
        ) + categories
            .filter { it.name != Category.FEATURED && it.list.isNotEmpty() }
            .onEach { category ->
                category.list.onEach { show ->
                    when (show) {
                        is Movie -> show.itemType = AppAdapter.Type.MOVIE_MOBILE_ITEM
                        is TvShow -> show.itemType = AppAdapter.Type.TV_SHOW_MOBILE_ITEM
                    }
                }
                category.itemSpacing = resources.getDimension(R.dimen.home_spacing).toInt()
                category.itemType = AppAdapter.Type.CATEGORY_MOBILE_ITEM
            }
        )
    }
}