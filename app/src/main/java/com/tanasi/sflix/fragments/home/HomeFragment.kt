package com.tanasi.sflix.fragments.home

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.leanback.app.BrowseSupportFragment
import com.tanasi.sflix.R

class HomeFragment : BrowseSupportFragment() {

    private val viewModel by viewModels<HomeViewModel>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        title = getString(R.string.app_name)

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                HomeViewModel.State.Loading -> {}
                is HomeViewModel.State.SuccessLoading -> {
                }
            }
        }
    }
}