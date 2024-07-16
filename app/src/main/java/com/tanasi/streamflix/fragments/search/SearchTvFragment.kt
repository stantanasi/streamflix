package com.tanasi.streamflix.fragments.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.tanasi.streamflix.R
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.database.AppDatabase
import com.tanasi.streamflix.databinding.FragmentSearchTvBinding
import com.tanasi.streamflix.models.Genre
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.utils.hideKeyboard
import com.tanasi.streamflix.utils.viewModelsFactory
import kotlinx.coroutines.launch

class SearchTvFragment : Fragment() {

    private var _binding: FragmentSearchTvBinding? = null
    private val binding get() = _binding!!

    private val database by lazy { AppDatabase.getInstance(requireContext()) }
    private val viewModel by viewModelsFactory { SearchViewModel(database) }

    private var appAdapter = AppAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchTvBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeSearch()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED).collect { state ->
                when (state) {
                    SearchViewModel.State.Searching -> {
                        binding.isLoading.apply {
                            root.visibility = View.VISIBLE
                            pbIsLoading.visibility = View.VISIBLE
                            gIsLoadingRetry.visibility = View.GONE
                        }
                        binding.vgvSearch.adapter = AppAdapter().also {
                            appAdapter = it
                        }
                    }
                    SearchViewModel.State.SearchingMore -> appAdapter.isLoading = true
                    is SearchViewModel.State.SuccessSearching -> {
                        displaySearch(state.results, state.hasMore)
                        appAdapter.isLoading = false
                        binding.etSearch.nextFocusDownId = binding.vgvSearch.id
                        binding.vgvSearch.visibility = View.VISIBLE
                        binding.isLoading.root.visibility = View.GONE
                    }
                    is SearchViewModel.State.FailedSearching -> {
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
                                    viewModel.search(viewModel.query)
                                }
                                binding.vgvSearch.visibility = View.INVISIBLE
                                binding.etSearch.nextFocusDownId = binding.isLoading.btnIsLoadingRetry.id
                                binding.isLoading.btnIsLoadingRetry.nextFocusUpId = binding.etSearch.id
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


    private fun initializeSearch() {
        binding.etSearch.apply {
            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_GO,
                    EditorInfo.IME_ACTION_SEARCH,
                    EditorInfo.IME_ACTION_SEND,
                    EditorInfo.IME_ACTION_NEXT,
                    EditorInfo.IME_ACTION_DONE -> {
                        viewModel.search(text.toString())
                        hideKeyboard()
                        true
                    }
                    else -> false
                }
            }
        }

        binding.btnSearchClear.setOnClickListener {
            binding.etSearch.setText("")
            viewModel.search("")
        }

        binding.vgvSearch.apply {
            adapter = appAdapter.apply {
                stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }
            setItemSpacing(requireContext().resources.getDimension(R.dimen.search_spacing).toInt())
        }

        binding.root.requestFocus()
    }

    private fun displaySearch(list: List<AppAdapter.Item>, hasMore: Boolean) {
        binding.vgvSearch.apply {
            setNumColumns(
                if (viewModel.query == "") 5
                else 6
            )
        }

        appAdapter.submitList(list.onEach {
            when (it) {
                is Genre -> it.itemType = AppAdapter.Type.GENRE_GRID_TV_ITEM
                is Movie -> it.itemType = AppAdapter.Type.MOVIE_GRID_TV_ITEM
                is TvShow -> it.itemType = AppAdapter.Type.TV_SHOW_GRID_TV_ITEM
            }
        })

        if (hasMore && viewModel.query != "") {
            appAdapter.setOnLoadMoreListener { viewModel.loadMore() }
        } else {
            appAdapter.setOnLoadMoreListener(null)
        }
    }
}