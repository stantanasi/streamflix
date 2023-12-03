package com.tanasi.streamflix.fragments.tv_show

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.databinding.FragmentTvShowBinding
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.utils.viewModelsFactory

class TvShowFragment : Fragment() {

    private var _binding: FragmentTvShowBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<TvShowFragmentArgs>()
    private val viewModel by viewModelsFactory { TvShowViewModel(args.id) }

    private val appAdapter = AppAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTvShowBinding.inflate(inflater, container, false)
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
                        state.error.message ?: "",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        appAdapter.onSaveInstanceState(binding.vgvTvShow)
        _binding = null
    }


    private fun initializeTvShow() {
        binding.vgvTvShow.apply {
            adapter = appAdapter
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

        appAdapter.items.apply {
            clear()
            add(tvShow.apply { itemType = AppAdapter.Type.TV_SHOW })
            if (tvShow.seasons.isNotEmpty())
                add(tvShow.clone().apply { itemType = AppAdapter.Type.TV_SHOW_SEASONS })
            if (tvShow.cast.isNotEmpty())
                add(tvShow.clone().apply { itemType = AppAdapter.Type.TV_SHOW_CASTS })
            if (tvShow.recommendations.isNotEmpty())
                add(tvShow.clone().apply { itemType = AppAdapter.Type.TV_SHOW_RECOMMENDATIONS })
        }
        appAdapter.notifyDataSetChanged()
    }
}