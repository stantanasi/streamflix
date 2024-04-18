package com.tanasi.streamflix.fragments.player

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.SubtitleConfiguration
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.tanasi.streamflix.R
import com.tanasi.streamflix.database.AppDatabase
import com.tanasi.streamflix.databinding.ContentExoControllerTvBinding
import com.tanasi.streamflix.databinding.FragmentPlayerTvBinding
import com.tanasi.streamflix.models.Episode
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.models.Video
import com.tanasi.streamflix.models.WatchItem
import com.tanasi.streamflix.utils.MediaServer
import com.tanasi.streamflix.utils.UserPreferences
import com.tanasi.streamflix.utils.filterNotNullValues
import com.tanasi.streamflix.utils.setMediaServerId
import com.tanasi.streamflix.utils.setMediaServers
import com.tanasi.streamflix.utils.viewModelsFactory
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class PlayerTvFragment : Fragment() {

    private var _binding: FragmentPlayerTvBinding? = null
    private val binding get() = _binding!!

    private val PlayerView.controller
        get() = ContentExoControllerTvBinding.bind(this.findViewById(R.id.cl_exo_controller))
    private val PlayerView.isControllerVisible
        get() = this.javaClass.getDeclaredField("controller").let {
            it.isAccessible = true
            val controller = it.get(this) as PlayerControlView
            controller.isVisible
        }

    private val args by navArgs<PlayerTvFragmentArgs>()
    private val database by lazy { AppDatabase.getInstance(requireContext()) }
    private val viewModel by viewModelsFactory { PlayerViewModel(args.videoType, args.id) }

    private lateinit var player: ExoPlayer
    private lateinit var dataSourceFactory: HttpDataSource.Factory
    private lateinit var mediaSession: MediaSession

    private var servers = listOf<Video.Server>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerTvBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeVideo()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED).collect { state ->
                when (state) {
                    PlayerViewModel.State.LoadingServers -> {}
                    is PlayerViewModel.State.SuccessLoadingServers -> {
                        servers = state.servers
                        player.playlistMetadata = MediaMetadata.Builder()
                            .setTitle(state.toString())
                            .setMediaServers(state.servers.map {
                                MediaServer(
                                    id = it.id,
                                    name = it.name,
                                )
                            })
                            .build()
                        binding.settings.setOnServerSelected { server ->
                            viewModel.getVideo(state.servers.find { server.id == it.id }!!)
                        }
                        viewModel.getVideo(state.servers.first())
                    }
                    is PlayerViewModel.State.FailedLoadingServers -> {
                        Toast.makeText(
                            requireContext(),
                            state.error.message ?: "",
                            Toast.LENGTH_LONG
                        ).show()
                        findNavController().navigateUp()
                    }

                    is PlayerViewModel.State.LoadingVideo -> {
                        player.setMediaItem(
                            MediaItem.Builder()
                                .setUri(Uri.parse(""))
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setMediaServerId(state.server.id)
                                        .build()
                                )
                                .build()
                        )
                    }
                    is PlayerViewModel.State.SuccessLoadingVideo -> {
                        displayVideo(state.video, state.server)
                    }
                    is PlayerViewModel.State.FailedLoadingVideo -> {
                        Toast.makeText(
                            requireContext(),
                            state.error.message ?: "",
                            Toast.LENGTH_LONG
                        ).show()
                        servers.getOrNull(servers.indexOf(state.server) + 1)?.let {
                            viewModel.getVideo(it)
                        }
                    }
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
        _binding = null
    }

    fun onBackPressed(): Boolean = when {
        binding.settings.isVisible -> {
            binding.settings.onBackPressed()
        }
        binding.pvPlayer.isControllerVisible -> {
            binding.pvPlayer.hideController()
            true
        }
        else -> false
    }


    private fun initializeVideo() {
        dataSourceFactory = DefaultHttpDataSource.Factory()
        player = ExoPlayer.Builder(requireContext())
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build().also { player ->
                player.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build(),
                    true,
                )

                mediaSession = MediaSession.Builder(requireContext(), player)
                    .build()
            }

        binding.pvPlayer.player = player
        binding.settings.player = player
        binding.settings.subtitleView = binding.pvPlayer.subtitleView

        binding.pvPlayer.subtitleView?.apply {
            setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * UserPreferences.captionTextSize)
            setStyle(UserPreferences.captionStyle)
        }

        binding.pvPlayer.controller.tvExoTitle.text = args.title

        binding.pvPlayer.controller.tvExoSubtitle.text = args.subtitle

        binding.pvPlayer.controller.exoProgress.setKeyTimeIncrement(10_000)

        binding.pvPlayer.controller.exoSettings.setOnClickListener {
            binding.settings.show()
        }
    }

    private fun displayVideo(video: Video, server: Video.Server) {
        val currentPosition = player.currentPosition

        dataSourceFactory.setDefaultRequestProperties(mapOf(
            "Referer" to video.referer,
        ).filterNotNullValues())

        player.setMediaItem(
            MediaItem.Builder()
                .setUri(Uri.parse(video.source))
                .setSubtitleConfigurations(video.subtitles.map {
                    SubtitleConfiguration.Builder(Uri.parse(it.file))
                        .setMimeType(MimeTypes.TEXT_VTT)
                        .setLabel(it.label)
                        .build()
                })
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setMediaServerId(server.id)
                        .build()
                )
                .build()
        )

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                binding.pvPlayer.keepScreenOn = isPlaying

                if (!isPlaying) {
                    val watchItem = when (val videoType = args.videoType as Video.Type) {
                        is Video.Type.Movie -> database.movieDao().getById(videoType.id)
                        is Video.Type.Episode -> database.episodeDao().getById(videoType.id)
                    }

                    when {
                        player.hasStarted() && !player.hasFinished() -> {
                            watchItem?.isWatched = false
                            watchItem?.watchedDate = null
                            watchItem?.watchHistory = WatchItem.WatchHistory(
                                lastEngagementTimeUtcMillis = System.currentTimeMillis(),
                                lastPlaybackPositionMillis = player.currentPosition,
                                durationMillis = player.duration,
                            )
                        }
                        player.hasFinished() -> {
                            watchItem?.isWatched = true
                            watchItem?.watchedDate = Calendar.getInstance()
                            watchItem?.watchHistory = null
                        }
                    }

                    when (val videoType = args.videoType as Video.Type) {
                        is Video.Type.Movie -> {
                            database.movieDao().update(watchItem as Movie)
                        }
                        is Video.Type.Episode -> {
                            if (player.hasFinished()) {
                                database.episodeDao().resetProgressionFromEpisode(videoType.id)
                            }
                            database.episodeDao().update(watchItem as Episode)
                        }
                    }
                }
            }
        })

        if (currentPosition == 0L) {
            val watchItem = when (val videoType = args.videoType as Video.Type) {
                is Video.Type.Movie -> database.movieDao().getById(videoType.id)
                is Video.Type.Episode -> database.episodeDao().getById(videoType.id)
            }
            val lastPlaybackPositionMillis = watchItem?.watchHistory
                ?.let { it.lastPlaybackPositionMillis - 10.seconds.inWholeMilliseconds }

            player.seekTo(lastPlaybackPositionMillis ?: 0)
        } else {
            player.seekTo(currentPosition)
        }

        player.prepare()
        player.play()
    }


    private fun ExoPlayer.hasStarted(): Boolean {
        return (this.currentPosition > (this.duration * 0.03) || this.currentPosition > 2.minutes.inWholeMilliseconds)
    }

    private fun ExoPlayer.hasFinished(): Boolean {
        return (this.currentPosition > (this.duration * 0.90))
    }
}