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

    private val list = mutableListOf<SflixAdapter.Item>()

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

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                SearchViewModel.State.Searching -> {}

                is SearchViewModel.State.SuccessSearching -> {
                    list.apply {
                        clear()
                        addAll(state.results)
                    }
                    displaySearch()
                }
                is SearchViewModel.State.FailedSearching -> {
                    Toast.makeText(
                        requireContext(),
                        state.error.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            val query = binding.etSearch.text.toString().trim()

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


    private fun displaySearch() {
        binding.vgvSearch.apply {
            adapter = SflixAdapter(list.onEach {
                when (it) {
                    is Movie -> it.itemType = SflixAdapter.Type.MOVIE_SEARCH
                    is TvShow -> it.itemType = SflixAdapter.Type.TV_SHOW_SEARCH
                }
            })
            setItemSpacing(requireContext().resources.getDimension(R.dimen.search_spacing).toInt())
        }
    }
}