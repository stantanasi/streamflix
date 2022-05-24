package com.tanasi.sflix.fragments.player

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.leanback.app.VideoSupportFragment
import androidx.leanback.app.VideoSupportFragmentGlueHost
import androidx.leanback.media.MediaPlayerAdapter
import androidx.leanback.media.PlaybackTransportControlGlue
import androidx.leanback.widget.Action
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.PlaybackControlsRow.FastForwardAction
import androidx.leanback.widget.PlaybackControlsRow.RewindAction
import androidx.navigation.fragment.navArgs
import com.tanasi.sflix.databinding.FragmentPlayerBinding
import com.tanasi.sflix.models.Video
import java.util.concurrent.TimeUnit


class PlayerFragment : Fragment() {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<PlayerFragmentArgs>()
    private val viewModel by viewModels<PlayerViewModel>()

    private val videoFragment = VideoSupportFragment()
    private lateinit var video: Video

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        parentFragmentManager
            .beginTransaction()
            .replace(binding.flPlayer.id, videoFragment)
            .commit()

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                PlayerViewModel.State.Loading -> {}
                is PlayerViewModel.State.SuccessLoading -> {
                    video = state.video
                    displayVideo()
                }
            }
        }

        viewModel.getVideo(args.linkId)
    }


    private fun displayVideo() {
        val playerGlue = VideoPlayerGlue(
            requireContext(),
            MediaPlayerAdapter(requireContext()),
        )
        playerGlue.host = VideoSupportFragmentGlueHost(videoFragment)
        playerGlue.title = args.title
        playerGlue.subtitle = args.description
        playerGlue.playerAdapter.setDataSource(Uri.parse(video.source))
        playerGlue.playWhenPrepared()
    }

    class VideoPlayerGlue(
        context: Context,
        playerAdapter: MediaPlayerAdapter,
    ) : PlaybackTransportControlGlue<MediaPlayerAdapter>(
        context,
        playerAdapter
    ) {

        override fun onCreatePrimaryActions(adapter: ArrayObjectAdapter) {
            super.onCreatePrimaryActions(adapter)

            adapter.add(RewindAction(context))
            adapter.add(FastForwardAction(context))
        }

        override fun onActionClicked(action: Action) {
            super.onActionClicked(action)

            when (action) {
                is RewindAction -> {
                    var newPosition = currentPosition - TimeUnit.SECONDS.toMillis(10)
                    newPosition = if (newPosition < 0) 0 else newPosition
                    playerAdapter.seekTo(newPosition)
                }
                is FastForwardAction -> {
                    if (duration > -1) {
                        var newPosition = currentPosition + TimeUnit.SECONDS.toMillis(30)
                        newPosition = if (newPosition > duration) duration else newPosition
                        playerAdapter.seekTo(newPosition)
                    }
                }
            }
        }
    }
}