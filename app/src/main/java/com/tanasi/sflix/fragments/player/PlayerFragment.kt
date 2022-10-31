package com.tanasi.sflix.fragments.player

import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.tvprovider.media.tv.TvContractCompat
import androidx.tvprovider.media.tv.WatchNextProgram
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaItem.SubtitleConfiguration
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.util.MimeTypes
import com.tanasi.sflix.R
import com.tanasi.sflix.databinding.ContentExoControllerBinding
import com.tanasi.sflix.databinding.FragmentPlayerBinding
import com.tanasi.sflix.models.Video
import com.tanasi.sflix.utils.map
import kotlin.time.Duration.Companion.minutes

class PlayerFragment : Fragment() {

    enum class VideoType {
        Movie,
        Episode;
    }

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!

    private val StyledPlayerView.controller
        get() = ContentExoControllerBinding.bind(this.findViewById(R.id.cl_exo_controller))

    private val args by navArgs<PlayerFragmentArgs>()
    private val viewModel by viewModels<PlayerViewModel>()

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSessionCompat

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        viewModel.getVideo(args.videoType, args.id)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeVideo()

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                PlayerViewModel.State.Loading -> {}

                is PlayerViewModel.State.SuccessLoading -> {
                    displayVideo(state.video)
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
    }

    override fun onPause() {
        super.onPause()
        player.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        player.release()
        mediaSession.release()
    }


    private fun initializeVideo() {
        player = ExoPlayer.Builder(requireContext()).build()

        mediaSession = MediaSessionCompat(requireContext(), "Player").apply {
            isActive = true

            MediaSessionConnector(this).also {
                it.setPlayer(player)
                it.setQueueNavigator(object : TimelineQueueNavigator(this) {
                    override fun getMediaDescription(player: Player, windowIndex: Int) =
                        MediaDescriptionCompat.Builder()
                            .setTitle(args.title)
                            .setSubtitle(args.subtitle)
                            .build()
                })
            }
        }


        binding.pvPlayer.player = player

        binding.pvPlayer.controller.tvExoTitle.text = args.title

        binding.pvPlayer.controller.tvExoSubtitle.text = args.subtitle

        binding.pvPlayer.controller.exoProgress.setKeyTimeIncrement(10 * 1000)
    }

    private fun displayVideo(video: Video) {
        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.CONTENT_TYPE_MOVIE)
                .build(),
            true,
        )

        player.setMediaItem(
            MediaItem.Builder()
                .setUri(Uri.parse(video.source))
                .setSubtitleConfigurations(video.subtitles.map {
                    SubtitleConfiguration.Builder(Uri.parse(it.file))
                        .setMimeType(MimeTypes.TEXT_VTT)
                        .setLabel(it.label)
                        .build()
                })
                .build()
        )

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                binding.pvPlayer.keepScreenOn = isPlaying
            }
        })

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying) {
                    val program = requireContext().contentResolver.query(
                        TvContractCompat.WatchNextPrograms.CONTENT_URI,
                        WatchNextProgram.PROJECTION,
                        null,
                        null,
                        null
                    )?.map { WatchNextProgram.fromCursor(it) }
                        ?.find { it.contentId == args.id }

                    when {
                        player.hasStarted() -> {
                            if (program == null) {
                                val builder = WatchNextProgram.Builder()
                                    .setTitle(args.title)
                                    .setDescription(args.subtitle)
                                    .setType(
                                        when (args.videoType as VideoType) {
                                            VideoType.Movie -> TvContractCompat.PreviewPrograms.TYPE_MOVIE
                                            VideoType.Episode -> TvContractCompat.PreviewPrograms.TYPE_TV_EPISODE
                                        }
                                    )
                                    .setWatchNextType(TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE)
                                    .setLastEngagementTimeUtcMillis(System.currentTimeMillis())
                                    .setLastPlaybackPositionMillis(player.currentPosition.toInt())
                                    .setDurationMillis(player.duration.toInt())
                                    .setContentId(args.id)

                                requireContext().contentResolver.insert(
                                    TvContractCompat.WatchNextPrograms.CONTENT_URI,
                                    builder.build().toContentValues(),
                                )
                            } else {
                                val builder = WatchNextProgram.Builder(program)
                                    .setLastEngagementTimeUtcMillis(System.currentTimeMillis())
                                    .setLastPlaybackPositionMillis(player.currentPosition.toInt())
                                    .setDurationMillis(player.duration.toInt())

                                requireContext().contentResolver.update(
                                    TvContractCompat.buildWatchNextProgramUri(program.id),
                                    builder.build().toContentValues(),
                                    null,
                                    null,
                                )
                            }
                        }
                        player.hasFinished() && program != null -> {
                            requireContext().contentResolver.delete(
                                TvContractCompat.buildWatchNextProgramUri(program.id),
                                null,
                                null,
                            )
                        }
                    }
                }
            }
        })

        player.prepare()
        player.play()
    }


    private fun ExoPlayer.hasStarted(): Boolean {
        return (this.currentPosition > (this.duration * 0.03) || this.currentPosition > 2.minutes.inWholeMilliseconds)
                && !this.hasFinished()
    }

    private fun ExoPlayer.hasFinished(): Boolean {
        return (this.currentPosition > (this.duration * 0.90))
    }
}