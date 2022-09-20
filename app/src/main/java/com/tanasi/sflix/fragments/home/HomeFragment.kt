package com.tanasi.sflix.fragments.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.tanasi.sflix.R
import com.tanasi.sflix.adapters.SflixAdapter
import com.tanasi.sflix.databinding.FragmentHomeBinding
import com.tanasi.sflix.models.Category
import com.tanasi.sflix.models.Movie
import com.tanasi.sflix.models.TvShow

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<HomeViewModel>()

    private val categories = mutableListOf<Category>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (_binding == null) {
            _binding = FragmentHomeBinding.inflate(inflater, container, false)
            viewModel.getHome()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                HomeViewModel.State.Loading -> {}

                is HomeViewModel.State.SuccessLoading -> {
                    categories.apply {
                        clear()
                        addAll(state.categories)
                    }
                    displayHome()
                }
                is HomeViewModel.State.FailedLoading -> {
                    Toast.makeText(
                        requireContext(),
                        state.error.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }


    private fun displayHome() {
        binding.vgvHome.apply {
            adapter = SflixAdapter(categories.onEach {
                it.list.onEach { show ->
                    show.itemType = when (show) {
                        is Movie -> SflixAdapter.Type.MOVIE_ITEM
                        is TvShow -> SflixAdapter.Type.TV_SHOW_ITEM
                    }
                }
                it.itemSpacing = resources.getDimension(R.dimen.home_spacing).toInt()
            })
            setItemSpacing(resources.getDimension(R.dimen.home_spacing).toInt() * 2)
        }
    }
}