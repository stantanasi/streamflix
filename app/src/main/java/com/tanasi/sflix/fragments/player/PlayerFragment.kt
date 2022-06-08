package com.tanasi.sflix.fragments.player

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaItem.SubtitleConfiguration
import com.google.android.exoplayer2.util.MimeTypes
import com.tanasi.sflix.databinding.FragmentPlayerBinding
import com.tanasi.sflix.models.Video

class PlayerFragment : Fragment() {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<PlayerFragmentArgs>()
    private val viewModel by viewModels<PlayerViewModel>()

    private lateinit var player: ExoPlayer
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

        player = ExoPlayer.Builder(requireContext()).build()
        binding.pvPlayer.player = player

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                PlayerViewModel.State.Loading -> {}

                is PlayerViewModel.State.SuccessLoading -> {
                    video = state.video
                    displayVideo()
                }
                is PlayerViewModel.State.FailedLoading -> {
                    Toast.makeText(
                        requireContext(),
                        state.error.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        viewModel.getVideo(args.linkId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        player.release()
    }


    private fun displayVideo() {
        player.setMediaItem(
            MediaItem.Builder()
                .setUri(Uri.parse(video.source))
                .setSubtitleConfigurations(video.subtitles.map {
                    SubtitleConfiguration.Builder(Uri.parse(it.file))
                        .setMimeType(MimeTypes.TEXT_VTT)
                        .setLanguage(it.name)
                        .build()
                })
                .build()
        )

        player.prepare()
        player.play()
    }
}