package com.tanasi.streamflix.fragments.player

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.tvprovider.media.tv.TvContractCompat
import androidx.tvprovider.media.tv.WatchNextProgram
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.SubtitleConfiguration
import androidx.media3.common.Player
import androidx.media3.common.AudioAttributes
import androidx.media3.session.MediaSession
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.PlayerView
import com.tanasi.streamflix.R
import com.tanasi.streamflix.databinding.ContentExoControllerBinding
import com.tanasi.streamflix.databinding.FragmentPlayerBinding
import com.tanasi.streamflix.models.Video
import com.tanasi.streamflix.utils.UserPreferences
import com.tanasi.streamflix.utils.map
import com.tanasi.streamflix.utils.viewModelsFactory
import kotlinx.parcelize.Parcelize
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@UnstableApi @SuppressLint("RestrictedApi")
class PlayerFragment : Fragment() {

    sealed class VideoType : Parcelable {
        @Parcelize
        data class Movie(
            val id: String,
            val title: String,
            val releaseDate: String,
            val poster: String,
        ) : VideoType()

        @Parcelize
        data class Episode(
            val id: String,
            val number: Int,
            val title: String,
            val poster: String?,
            val tvShow: TvShow,
            val season: Season,
        ) : VideoType() {
            @Parcelize
            data class TvShow(
                val id: String,
                val title: String,
                val poster: String?,
                val banner: String?,
            ) : Parcelable

            @Parcelize
            data class Season(
                val number: Int,
                val title: String,
            ) : Parcelable
        }
    }

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!

    private val PlayerView.controller
        get() = ContentExoControllerBinding.bind(this.findViewById(R.id.cl_exo_controller))
    private val PlayerView.isControllerVisible
        get() = this.javaClass.getDeclaredField("controller").let {
            it.isAccessible = true
            val controller = it.get(this) as PlayerControlView
            controller.isVisible
        }

    private val args by navArgs<PlayerFragmentArgs>()
    private val viewModel by viewModelsFactory { PlayerViewModel(args.videoType, args.id) }

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession

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
                        state.error.message ?: "",
                        Toast.LENGTH_LONG
                    ).show()
                    findNavController().navigateUp()
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
            true
        }
        binding.pvPlayer.isControllerVisible -> {
            binding.pvPlayer.hideController()
            true
        }
        else -> false
    }


    private fun initializeVideo() {
        player = ExoPlayer.Builder(requireContext()).build()
        mediaSession = MediaSession.Builder(requireContext(), player)
            .build()
        binding.pvPlayer.player = player
        binding.settings.player = player

        binding.pvPlayer.subtitleView?.apply {
            setStyle(
                CaptionStyleCompat(
                    Color.WHITE,
                    Color.argb(128, 0, 0, 0),
                    Color.TRANSPARENT,
                    CaptionStyleCompat.EDGE_TYPE_NONE,
                    Color.WHITE,
                    null
                )
            )
        }

        binding.pvPlayer.controller.tvExoTitle.text = args.title

        binding.pvPlayer.controller.tvExoSubtitle.text = args.subtitle

        binding.pvPlayer.controller.exoProgress.setKeyTimeIncrement(10_000)

        binding.pvPlayer.controller.exoSettings.setOnClickListener {
            binding.settings.show()
        }
    }

    private fun displayVideo(video: Video) {
        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            true,
        )
        player.setMediaItem(
            MediaItem.Builder()
                .setUri(Uri.parse(video.sources.firstOrNull() ?: ""))
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

                if (!isPlaying) {
                    val program = requireContext().contentResolver.query(
                        TvContractCompat.WatchNextPrograms.CONTENT_URI,
                        WatchNextProgram.PROJECTION,
                        null,
                        null,
                        null
                    )?.map { WatchNextProgram.fromCursor(it) }
                        ?.find { it.contentId == args.id && it.internalProviderId == UserPreferences.currentProvider!!.name }

                    when {
                        player.hasStarted() -> {
                            if (program == null) {
                                val builder = WatchNextProgram.Builder()
                                    .setTitle(args.title)
                                    .setDescription(args.subtitle)
                                    .setWatchNextType(TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE)
                                    .setLastEngagementTimeUtcMillis(System.currentTimeMillis())
                                    .setLastPlaybackPositionMillis(player.currentPosition.toInt())
                                    .setDurationMillis(player.duration.toInt())
                                    .setContentId(args.id)
                                    .setInternalProviderId(UserPreferences.currentProvider!!.name)

                                when (val videoType = args.videoType as VideoType) {
                                    is VideoType.Movie -> {
                                        builder
                                            .setType(TvContractCompat.PreviewPrograms.TYPE_MOVIE)
                                            .setReleaseDate(videoType.releaseDate)
                                            .setPosterArtUri(Uri.parse(videoType.poster))
                                    }
                                    is VideoType.Episode -> {
                                        builder
                                            .setType(TvContractCompat.PreviewPrograms.TYPE_TV_EPISODE)
                                            .setSeriesId(videoType.tvShow.id)
                                            .setEpisodeNumber(videoType.number)
                                            .setEpisodeTitle(videoType.title)
                                            .setSeasonNumber(videoType.season.number)
                                            .setSeasonTitle(videoType.season.title)
                                            .setPosterArtUri(
                                                Uri.parse(
                                                    videoType.tvShow.poster
                                                        ?: videoType.tvShow.banner
                                                )
                                            )
                                    }
                                }

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

        val lastPlaybackPositionMillis = requireContext().contentResolver.query(
            TvContractCompat.WatchNextPrograms.CONTENT_URI,
            WatchNextProgram.PROJECTION,
            null,
            null,
            null
        )?.map { WatchNextProgram.fromCursor(it) }
            ?.find { it.contentId == args.id && it.internalProviderId == UserPreferences.currentProvider!!.name }
            ?.let { it.lastPlaybackPositionMillis.toLong() - 10.seconds.inWholeMilliseconds }

        player.seekTo(lastPlaybackPositionMillis ?: 0)

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