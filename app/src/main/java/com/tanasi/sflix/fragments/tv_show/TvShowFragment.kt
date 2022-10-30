package com.tanasi.sflix.fragments.tv_show

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.tanasi.sflix.adapters.SflixAdapter
import com.tanasi.sflix.databinding.FragmentTvShowBinding
import com.tanasi.sflix.models.TvShow

class TvShowFragment : Fragment() {

    private var _binding: FragmentTvShowBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<TvShowViewModel>()
    private val args by navArgs<TvShowFragmentArgs>()

    private val sflixAdapter = SflixAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (_binding == null) {
            _binding = FragmentTvShowBinding.inflate(inflater, container, false)
            viewModel.getTvShowById(args.id)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeTvShow()

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                TvShowViewModel.State.Loading -> binding.isLoading.root.visibility = View.VISIBLE

                is TvShowViewModel.State.SuccessLoading -> {
                    displayTvShow(state.tvShow)
                    binding.isLoading.root.visibility = View.GONE
                }
                is TvShowViewModel.State.FailedLoading -> {
                    Toast.makeText(
                        requireContext(),
                        state.error.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }


    private fun initializeTvShow() {
        binding.vgvTvShow.apply {
            adapter = sflixAdapter
            setItemSpacing(80)
        }
    }

    private fun displayTvShow(tvShow: TvShow) {
        Glide.with(requireContext())
            .load(tvShow.banner)
            .into(binding.ivTvShowBanner)

        tvShow.seasons.onEach {
            it.tvShow = tvShow
        }

        sflixAdapter.items.apply {
            clear()
            add(tvShow.apply { itemType = SflixAdapter.Type.TV_SHOW })
            if (tvShow.seasons.isNotEmpty())
                add(tvShow.clone().apply { itemType = SflixAdapter.Type.TV_SHOW_SEASONS })
            if (tvShow.cast.isNotEmpty())
                add(tvShow.clone().apply { itemType = SflixAdapter.Type.TV_SHOW_CASTS })
            if (tvShow.recommendations.isNotEmpty())
                add(tvShow.clone().apply { itemType = SflixAdapter.Type.TV_SHOW_RECOMMENDATIONS })
        }
        sflixAdapter.notifyDataSetChanged()
    }
}