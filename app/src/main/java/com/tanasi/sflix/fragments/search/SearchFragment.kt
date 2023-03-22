package com.tanasi.sflix.fragments.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.tanasi.sflix.R
import com.tanasi.sflix.adapters.SflixAdapter
import com.tanasi.sflix.databinding.FragmentSearchBinding
import com.tanasi.sflix.models.Movie
import com.tanasi.sflix.models.TvShow
import com.tanasi.sflix.utils.hideKeyboard

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<SearchViewModel>()

    private val sflixAdapter = SflixAdapter()

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
            nextFocusDownId = when {
                sflixAdapter.items.isNotEmpty() -> binding.vgvSearch.id
                else -> binding.etSearch.id
            }

            setOnEditorActionListener { _, actionId, _ ->
                val query = text.toString().trim()

                when (actionId) {
                    EditorInfo.IME_ACTION_SEARCH -> {
                        viewModel.search(query)
                        hideKeyboard()
                        true
                    }
                    else -> false
                }
            }
        }

        binding.vgvSearch.apply {
            adapter = sflixAdapter
            setItemSpacing(requireContext().resources.getDimension(R.dimen.search_spacing).toInt())

            isFocusable = sflixAdapter.items.isNotEmpty()
            isFocusableInTouchMode = sflixAdapter.items.isNotEmpty()
        }
    }

    private fun displaySearch(list: List<SflixAdapter.Item>) {
        sflixAdapter.items.apply {
            clear()
            addAll(list.onEach {
                when (it) {
                    is Movie -> it.itemType = SflixAdapter.Type.MOVIE_GRID_ITEM
                    is TvShow -> it.itemType = SflixAdapter.Type.TV_SHOW_GRID_ITEM
                }
            })
        }
        sflixAdapter.notifyDataSetChanged()

        binding.etSearch.nextFocusDownId = when {
            sflixAdapter.items.isNotEmpty() -> binding.vgvSearch.id
            else -> binding.etSearch.id
        }

        binding.vgvSearch.apply {
            isFocusable = sflixAdapter.items.isNotEmpty()
            isFocusableInTouchMode = sflixAdapter.items.isNotEmpty()
        }
    }
}