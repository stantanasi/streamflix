package com.tanasi.sflix.fragments.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.tanasi.sflix.R
import com.tanasi.sflix.adapters.SflixAdapter
import com.tanasi.sflix.databinding.FragmentHomeBinding
import com.tanasi.sflix.models.Category
import com.tanasi.sflix.models.Movie
import com.tanasi.sflix.models.TvShow

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    val binding get() = _binding!!

    private val viewModel by viewModels<HomeViewModel>()

    private val sflixAdapter = SflixAdapter()

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
                        state.error.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }


    fun updateBackground(uri: String?) {
        Glide.with(requireContext())
            .load(uri)
            .into(binding.ivHomeBackground)
    }

    private fun initializeHome() {
        binding.vgvHome.apply {
            adapter = sflixAdapter
            setItemSpacing(resources.getDimension(R.dimen.home_spacing).toInt() * 2)
        }
    }

    private fun displayHome(categories: List<Category>) {
        sflixAdapter.items.apply {
            clear()
            addAll(categories.onEach { category ->
                category.list.onEach { show ->
                    show.itemType = when (show) {
                        is Movie -> SflixAdapter.Type.MOVIE_ITEM
                        is TvShow -> SflixAdapter.Type.TV_SHOW_ITEM
                    }
                }
                category.itemSpacing = resources.getDimension(R.dimen.home_spacing).toInt()
                category.itemType = when (category.name) {
                    "Featured" -> SflixAdapter.Type.CATEGORY_SWIPER
                    else -> SflixAdapter.Type.CATEGORY_ITEM
                }
            })
        }
        sflixAdapter.notifyDataSetChanged()
    }
}