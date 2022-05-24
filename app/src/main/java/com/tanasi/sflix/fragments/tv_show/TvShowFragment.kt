package com.tanasi.sflix.fragments.tv_show

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.tanasi.sflix.databinding.FragmentTvShowBinding
import com.tanasi.sflix.models.TvShow
import com.tanasi.sflix.presenters.EpisodePresenter
import com.tanasi.sflix.utils.format

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
        _binding = FragmentTvShowBinding.inflate(inflater, container, false)
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
            }
        }

        viewModel.fetchTvShow(args.id)
    }


    private fun displayTvShow() {
        Glide.with(requireContext())
            .load(tvShow.banner)
            .into(binding.ivTvShowBanner)

        Glide.with(requireContext())
            .load(tvShow.poster)
            .into(binding.ivTvShowPoster)

        binding.tvTvShowTitle.text = tvShow.title

        binding.tvTvShowQuality.text = tvShow.quality?.name ?: "N/A"

        binding.tvTvShowOverview.text = tvShow.overview

        binding.tvTvShowReleased.text = tvShow.released?.format("yyyy-MM-dd")

        binding.tvTvShowRuntime.text = "${tvShow.runtime} min"

        val rowsFragment = RowsSupportFragment()

        parentFragmentManager
            .beginTransaction()
            .replace(binding.flTvShow.id, rowsFragment)
            .commit()

        rowsFragment.adapter = ArrayObjectAdapter(ListRowPresenter()).apply {
            tvShow.seasons.forEach { season ->
                add(ListRow(
                    HeaderItem(season.title),
                    ArrayObjectAdapter(EpisodePresenter()).apply {
                        clear()
                        addAll(0, season.episodes)
                    }
                ))
            }
        }
    }
}