package com.tanasi.streamflix.fragments.people

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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.tanasi.streamflix.R
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.database.AppDatabase
import com.tanasi.streamflix.databinding.FragmentPeopleMobileBinding
import com.tanasi.streamflix.databinding.HeaderPeopleMobileBinding
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.models.People
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.ui.SpacingItemDecoration
import com.tanasi.streamflix.utils.dp
import com.tanasi.streamflix.utils.format
import com.tanasi.streamflix.utils.viewModelsFactory
import kotlinx.coroutines.launch

class PeopleMobileFragment : Fragment() {

    private var _binding: FragmentPeopleMobileBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<PeopleMobileFragmentArgs>()
    private val database by lazy { AppDatabase.getInstance(requireContext()) }
    private val viewModel by viewModelsFactory { PeopleViewModel(args.id, database) }

    private val appAdapter = AppAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPeopleMobileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializePeople()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED).collect { state ->
                when (state) {
                    PeopleViewModel.State.Loading -> binding.isLoading.apply {
                        root.visibility = View.VISIBLE
                        pbIsLoading.visibility = View.VISIBLE
                        gIsLoadingRetry.visibility = View.GONE
                    }
                    PeopleViewModel.State.LoadingMore -> appAdapter.isLoading = true
                    is PeopleViewModel.State.SuccessLoading -> {
                        displayPeople(state.people, state.hasMore)
                        appAdapter.isLoading = false
                        binding.isLoading.root.visibility = View.GONE
                    }
                    is PeopleViewModel.State.FailedLoading -> {
                        Toast.makeText(
                            requireContext(),
                            state.error.message ?: "",
                            Toast.LENGTH_SHORT
                        ).show()
                        if (appAdapter.isLoading) {
                            appAdapter.isLoading = false
                        } else {
                            binding.isLoading.apply {
                                pbIsLoading.visibility = View.GONE
                                gIsLoadingRetry.visibility = View.VISIBLE
                                btnIsLoadingRetry.setOnClickListener {
                                    viewModel.getPeople(args.id)
                                }
                            }
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


    private fun initializePeople() {
        binding.rvPeople.apply {
            layoutManager = GridLayoutManager(context, 3).also {
                it.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        val viewType = appAdapter.getItemViewType(position)
                        return when (AppAdapter.Type.entries[viewType]) {
                            AppAdapter.Type.HEADER -> it.spanCount
                            else -> 1
                        }
                    }
                }
            }
            adapter = appAdapter.apply {
                stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }
            addItemDecoration(
                SpacingItemDecoration(10.dp(requireContext()))
            )
        }
    }

    private fun displayPeople(people: People, hasMore: Boolean) {
        appAdapter.setHeader(
            binding = { parent ->
                HeaderPeopleMobileBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            },
            bind = { binding ->
                binding.ivPeopleImage.apply {
                    clipToOutline = true
                    Glide.with(context)
                        .load(people.image ?: args.image)
                        .placeholder(R.drawable.ic_person_placeholder)
                        .centerCrop()
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(this)
                }

                binding.tvPeopleName.text = people.name.takeIf { it.isNotEmpty() } ?: args.name

                binding.tvPeopleBirthday.text = people.birthday?.format("MMMM dd, yyyy")

                binding.gPeopleBirthday.visibility = when {
                    binding.tvPeopleBirthday.text.isNullOrEmpty() -> View.GONE
                    else -> View.VISIBLE
                }

                binding.tvPeopleDeathday.text = people.deathday?.format("MMMM dd, yyyy")

                binding.gPeopleDeathday.visibility = when {
                    binding.tvPeopleDeathday.text.isNullOrEmpty() -> View.GONE
                    else -> View.VISIBLE
                }

                binding.tvPeopleBirthplace.text = people.placeOfBirth

                binding.gPeopleBirthplace.visibility = when {
                    binding.tvPeopleBirthplace.text.isNullOrEmpty() -> View.GONE
                    else -> View.VISIBLE
                }

                binding.tvPeopleBiography.apply {
                    maxLines = 7
                    text = people.biography
                }

                binding.tvPeopleBiographyReadMore.apply {
                    setOnClickListener {
                        binding.tvPeopleBiography.maxLines = Int.MAX_VALUE
                        binding.tvPeopleBiographyReadMore.visibility = View.GONE
                    }

                    binding.tvPeopleBiography.post {
                        visibility = when {
                            binding.tvPeopleBiography.lineCount > 7 -> View.VISIBLE
                            else -> View.GONE
                        }
                    }
                }

                binding.gPeopleBiography.visibility = when {
                    binding.tvPeopleBiography.text.isNullOrEmpty() -> View.GONE
                    else -> View.VISIBLE
                }
            }
        )

        appAdapter.submitList(people.filmography.onEach {
            when (it) {
                is Movie -> it.itemType = AppAdapter.Type.MOVIE_GRID_MOBILE_ITEM
                is TvShow -> it.itemType = AppAdapter.Type.TV_SHOW_GRID_MOBILE_ITEM
            }
        })

        if (hasMore) {
            appAdapter.setOnLoadMoreListener { viewModel.loadMorePeopleFilmography() }
        } else {
            appAdapter.setOnLoadMoreListener(null)
        }
    }
}