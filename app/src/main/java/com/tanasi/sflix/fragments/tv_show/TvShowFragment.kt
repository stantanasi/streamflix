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

    private lateinit var tvShow: TvShow

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (_binding == null) {
            _binding = FragmentTvShowBinding.inflate(inflater, container, false)
            viewModel.fetchTvShow(args.id)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                TvShowViewModel.State.Loading -> {}

                is TvShowViewModel.State.SuccessLoading -> {
                    tvShow = state.tvShow
                    displayTvShow()
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


    private fun displayTvShow() {
        Glide.with(requireContext())
            .load(tvShow.banner)
            .into(binding.ivTvShowBanner)

        binding.vgvTvShow.apply {
            adapter = SflixAdapter(mutableListOf<SflixAdapter.Item>().also {
                it.add(tvShow.apply { itemType = SflixAdapter.Type.TV_SHOW_HEADER })
                it.add(tvShow.clone().apply { itemType = SflixAdapter.Type.TV_SHOW_CASTS })
            })
            setItemSpacing(80)
        }
    }
}