package com.tanasi.sflix.fragments.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.tanasi.sflix.adapters.SflixAdapter
import com.tanasi.sflix.databinding.FragmentSearchBinding
import com.tanasi.sflix.models.Movie
import com.tanasi.sflix.models.TvShow
import java.util.*

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

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            private var timer = Timer()

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                val query = s.toString().trim()
                timer.cancel()
                timer = Timer()
                timer.schedule(object : TimerTask() {
                    override fun run() {
                        viewModel.search(query)
                    }
                }, 1000)
            }
        })
    }


    private fun displaySearch() {
        binding.rvSearch.adapter = SflixAdapter(list.map {
            when (it) {
                is Movie -> it.itemType = SflixAdapter.Type.MOVIE
                is TvShow -> it.itemType = SflixAdapter.Type.TV_SHOW
            }
            it
        })
    }
}