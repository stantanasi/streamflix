package com.tanasi.streamflix.fragments.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.tvprovider.media.tv.TvContractCompat
import androidx.tvprovider.media.tv.WatchNextProgram
import com.bumptech.glide.Glide
import com.tanasi.streamflix.R
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.databinding.FragmentHomeBinding
import com.tanasi.streamflix.models.*
import com.tanasi.streamflix.utils.UserPreferences
import com.tanasi.streamflix.utils.map

@SuppressLint("RestrictedApi")
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    val binding get() = _binding!!

    private val viewModel by viewModels<HomeViewModel>()

    private val appAdapter = AppAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeHome()

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                HomeViewModel.State.Loading -> binding.isLoading.root.visibility = View.VISIBLE
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
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    fun updateBackground(uri: String?) {
        Glide.with(requireContext())
            .load(uri)
            .into(binding.ivHomeBackground)
    }

    private fun initializeHome() {
        binding.vgvHome.apply {
            adapter = appAdapter
            setItemSpacing(resources.getDimension(R.dimen.home_spacing).toInt() * 2)
        }
    }

    private fun displayHome(categories: List<Category>) {
        appAdapter.items.apply {
            clear()

            categories.find { it.name == Category.FEATURED }?.let { category ->
                category.itemType = AppAdapter.Type.CATEGORY_SWIPER
                add(category)
            }

            Category(
                name = getString(R.string.home_continue_watching),
                list = requireContext().contentResolver.query(
                    TvContractCompat.WatchNextPrograms.CONTENT_URI,
                    WatchNextProgram.PROJECTION,
                    null,
                    null,
                    null
                )?.map { WatchNextProgram.fromCursor(it) }
                    ?.filter { it.internalProviderId == UserPreferences.currentProvider!!.name }
                    ?.sortedBy { it.lastEngagementTimeUtcMillis }?.reversed()
                    ?.mapNotNull {
                        when (it.type) {
                            TvContractCompat.PreviewPrograms.TYPE_MOVIE -> Movie(
                                id = it.contentId ?: "",
                                title = it.title ?: "",
                                released = it.releaseDate,
                                poster = it.posterArtUri?.toString(),
                            )
                            TvContractCompat.PreviewPrograms.TYPE_TV_EPISODE -> Episode(
                                id = it.contentId,
                                number = it.episodeNumber?.toIntOrNull() ?: 0,
                                title = it.episodeTitle ?: "",

                                tvShow = TvShow(
                                    id = it.seriesId ?: "",
                                    title = it.title ?: "",
                                    poster = it.posterArtUri?.toString(),
                                ),
                                season = Season(
                                    id = "",
                                    number = it.seasonNumber?.toIntOrNull() ?: 0,
                                    title = it.seasonTitle ?: "",
                                ),
                            )
                            else -> null
                        }
                    } ?: listOf()
            ).takeIf { it.list.isNotEmpty() }?.let {
                it.list.onEach { show ->
                    when (show) {
                        is Movie -> show.itemType = AppAdapter.Type.MOVIE_CONTINUE_WATCHING_ITEM
                        is Episode -> show.itemType = AppAdapter.Type.EPISODE_CONTINUE_WATCHING_ITEM
                    }
                }
                it.itemSpacing = resources.getDimension(R.dimen.home_spacing).toInt()
                it.itemType = AppAdapter.Type.CATEGORY_ITEM
                add(it)
            }

            categories
                .filter { it.name != Category.FEATURED && it.list.isNotEmpty() }
                .onEach { category ->
                    category.list.onEach { show ->
                        when (show) {
                            is Movie -> show.itemType = AppAdapter.Type.MOVIE_ITEM
                            is TvShow -> show.itemType = AppAdapter.Type.TV_SHOW_ITEM
                        }
                    }
                    category.itemSpacing = resources.getDimension(R.dimen.home_spacing).toInt()
                    category.itemType = AppAdapter.Type.CATEGORY_ITEM
                    add(category)
                }
        }
        appAdapter.notifyDataSetChanged()
    }
}