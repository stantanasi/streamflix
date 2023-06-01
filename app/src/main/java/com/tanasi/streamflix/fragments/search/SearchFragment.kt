package com.tanasi.streamflix.fragments.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.tanasi.streamflix.R
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.databinding.FragmentSearchBinding
import com.tanasi.streamflix.models.Genre
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.utils.hideKeyboard

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<SearchViewModel>()

    private val appAdapter = AppAdapter()

    private var query = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeSearch()

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                SearchViewModel.State.Searching -> binding.isLoading.root.visibility = View.VISIBLE
                is SearchViewModel.State.SuccessSearching -> {
                    displaySearch(state.results)
                    binding.isLoading.root.visibility = View.GONE
                }
                is SearchViewModel.State.FailedSearching -> {
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


    private fun initializeSearch() {
        binding.etSearch.apply {
            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_SEARCH -> {
                        query = text.toString()
                        viewModel.search(query)
                        hideKeyboard()
                        true
                    }
                    else -> false
                }
            }
        }

        binding.btnSearchClear.setOnClickListener {
            if (query.isNotEmpty()) {
                query = ""
                binding.etSearch.setText(query)
                viewModel.search(query)
            }
        }

        binding.vgvSearch.apply {
            adapter = appAdapter
            setItemSpacing(requireContext().resources.getDimension(R.dimen.search_spacing).toInt())
        }
    }

    private fun displaySearch(list: List<AppAdapter.Item>) {
        appAdapter.items.apply {
            clear()
            addAll(list.onEach {
                when (it) {
                    is Genre -> it.itemType = AppAdapter.Type.GENRE_GRID_ITEM
                    is Movie -> it.itemType = AppAdapter.Type.MOVIE_GRID_ITEM
                    is TvShow -> it.itemType = AppAdapter.Type.TV_SHOW_GRID_ITEM
                }
            })
        }
        appAdapter.notifyDataSetChanged()

        binding.vgvSearch.apply {
            setNumColumns(
                when (query) {
                    "" -> 5
                    else -> 6
                }
            )
        }
    }
}