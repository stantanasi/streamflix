package com.tanasi.streamflix.ui

import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Tracks
import com.google.android.exoplayer2.trackselection.TrackSelectionOverride
import com.google.android.exoplayer2.ui.DefaultTrackNameProvider
import com.tanasi.streamflix.R
import com.tanasi.streamflix.databinding.ItemSettingBinding
import com.tanasi.streamflix.databinding.ViewPlayerSettingsBinding
import com.tanasi.streamflix.utils.findClosest
import com.tanasi.streamflix.utils.trackFormats

class PlayerSettingsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    val binding = ViewPlayerSettingsBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

    var player: ExoPlayer? = null
        set(value) {
            if (field === value) return

            value?.addListener(object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    if (events.contains(Player.EVENT_TRACKS_CHANGED)) {
                        Setting.Quality.init(value, resources)
                        Setting.Subtitle.init(value, resources)
                    }
                    if (events.contains(Player.EVENT_PLAYBACK_PARAMETERS_CHANGED)) {
                        Setting.Speed.refresh(value)
                    }
                }
            })
            value?.let { Setting.Speed.refresh(it) }

            field = value
        }


    init {
        Setting.init(this)
    }

    fun onBackPressed() {
        val adapter = binding.rvSettings.adapter as? SettingsAdapter
        when (adapter?.setting) {
            Setting.Main -> hide()
            else -> displaySetting(Setting.Main)
        }
    }

    override fun focusSearch(focused: View, direction: Int): View {
        return when {
            binding.rvSettings.hasFocus() -> focused
            else -> super.focusSearch(focused, direction)
        }
    }


    fun show() {
        this.visibility = View.VISIBLE

        displaySetting(Setting.Main)
    }

    private fun displaySetting(setting: Setting) {
        binding.tvSettingsHeader.apply {
            text = when (setting) {
                is Setting.Main -> context.getString(R.string.player_settings_title)
                is Setting.Quality -> context.getString(R.string.player_settings_quality_title)
                is Setting.Subtitle -> context.getString(R.string.player_settings_subtitles_title)
                is Setting.Speed -> context.getString(R.string.player_settings_speed_title)
            }
        }

        binding.rvSettings.adapter = setting.adapter
        binding.rvSettings.requestFocus()
    }

    fun hide() {
        this.visibility = View.GONE
    }


    private sealed class Setting {

        lateinit var adapter: SettingsAdapter

        object Main : Setting() {
            val list = listOf(Quality, Subtitle, Speed)
        }

        object Quality : Setting() {
            val list = mutableListOf<PlayerSettingsView.Quality>()

            val selected: PlayerSettingsView.Quality
                get() = list.find { it.isSelected } ?: PlayerSettingsView.Quality.Auto

            fun init(player: ExoPlayer, resources: Resources) {
                list.clear()
                list.add(PlayerSettingsView.Quality.Auto)
                list.addAll(
                    player.currentTracks.groups
                        .filter { it.type == C.TRACK_TYPE_VIDEO }
                        .flatMap { trackGroup ->
                            trackGroup.trackFormats
                                .filter { it.selectionFlags and C.SELECTION_FLAG_FORCED == 0 }
                                .distinctBy { it.width to it.height }
                                .map { trackFormat ->
                                    PlayerSettingsView.Quality.VideoTrackInformation(
                                        name = DefaultTrackNameProvider(resources)
                                            .getTrackName(trackFormat),
                                        width = trackFormat.width,
                                        height = trackFormat.height,
                                        bitrate = trackFormat.bitrate,

                                        player = player,
                                    )
                                }
                        }
                        .sortedByDescending { it.height }
                )
            }
        }

        object Subtitle : Setting() {
            val list = mutableListOf<PlayerSettingsView.Subtitle>()

            val selected: PlayerSettingsView.Subtitle
                get() = list.find { it.isSelected } ?: PlayerSettingsView.Subtitle.None

            fun init(player: ExoPlayer, resources: Resources) {
                list.clear()
                list.add(PlayerSettingsView.Subtitle.None)
                list.addAll(
                    player.currentTracks.groups
                        .filter { it.type == C.TRACK_TYPE_TEXT }
                        .flatMap { trackGroup ->
                            trackGroup.trackFormats
                                .filter { it.selectionFlags and C.SELECTION_FLAG_FORCED == 0 }
                                .filter { it.label != null }
                                .mapIndexed { trackIndex, trackFormat ->
                                    PlayerSettingsView.Subtitle.TextTrackInformation(
                                        name = DefaultTrackNameProvider(resources)
                                            .getTrackName(trackFormat),

                                        trackGroup = trackGroup,
                                        trackIndex = trackIndex,
                                    )
                                }
                        }
                        .sortedBy { it.name }
                )
            }
        }

        object Speed : Setting() {
            val list = listOf(
                PlaybackSpeed(R.string.player_settings_speed_0_25, 0.25F),
                PlaybackSpeed(R.string.player_settings_speed_0_5, 0.5F),
                PlaybackSpeed(R.string.player_settings_speed_0_75, 0.75F),
                PlaybackSpeed(R.string.player_settings_speed_1, 1F),
                PlaybackSpeed(R.string.player_settings_speed_1_25, 1.25F),
                PlaybackSpeed(R.string.player_settings_speed_1_5, 1.5F),
                PlaybackSpeed(R.string.player_settings_speed_1_75, 1.75F),
                PlaybackSpeed(R.string.player_settings_speed_2, 2F),
            )

            val selected: PlaybackSpeed
                get() = list.find { it.isSelected }
                    ?: list.find { it.speed == 1F }
                    ?: PlaybackSpeed(R.string.player_settings_speed_1, 1F)

            fun refresh(player: ExoPlayer) {
                list.forEach { it.isSelected = false }
                list.findClosest(player.playbackParameters.speed) { it.speed }?.let {
                    it.isSelected = true
                }
            }
        }


        companion object {
            fun init(playerSettingsView: PlayerSettingsView) {
                values().forEach { setting ->
                    setting.adapter = SettingsAdapter(setting).also {
                        it.playerSettingsView = playerSettingsView
                    }
                }
            }

            fun values() = listOf(Main, Quality, Subtitle, Speed)
        }
    }


    private class SettingsAdapter(
        val setting: Setting,
    ) : RecyclerView.Adapter<SettingViewHolder>() {

        lateinit var playerSettingsView: PlayerSettingsView

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            SettingViewHolder(
                ItemSettingBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        override fun onBindViewHolder(holder: SettingViewHolder, position: Int) {
            holder.playerSettingsView = playerSettingsView

            when (setting) {
                is Setting.Main -> holder.displayMainSetting(setting.list[position])
                is Setting.Quality -> holder.displayQualitySetting(setting.list[position])
                is Setting.Subtitle -> holder.displaySubtitleSetting(setting.list[position])
                is Setting.Speed -> holder.displaySpeedSetting(setting.list[position])
            }
        }

        override fun getItemCount() = when (setting) {
            is Setting.Main -> setting.list.size
            is Setting.Quality -> setting.list.size
            is Setting.Subtitle -> setting.list.size
            is Setting.Speed -> setting.list.size
        }
    }

    private class SettingViewHolder(
        val binding: ItemSettingBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        lateinit var playerSettingsView: PlayerSettingsView

        fun displayMainSetting(setting: Setting) {
            binding.root.setOnClickListener {
                playerSettingsView.displaySetting(setting)
            }

            binding.ivSettingIcon.apply {
                when (setting) {
                    is Setting.Quality -> setImageDrawable(
                        ContextCompat.getDrawable(context, R.drawable.ic_settings_quality)
                    )
                    is Setting.Subtitle -> setImageDrawable(
                        ContextCompat.getDrawable(
                            context,
                            when (setting.selected) {
                                is Subtitle.None -> R.drawable.ic_settings_subtitle_off
                                is Subtitle.TextTrackInformation -> R.drawable.ic_settings_subtitle_on
                            }
                        )
                    )
                    is Setting.Speed -> setImageDrawable(
                        ContextCompat.getDrawable(context, R.drawable.ic_settings_playback_speed)
                    )
                    else -> {}
                }
                visibility = View.VISIBLE
            }

            binding.tvSettingMainText.apply {
                text = when (setting) {
                    is Setting.Quality -> context.getString(R.string.player_settings_quality_label)
                    is Setting.Subtitle -> context.getString(R.string.player_settings_subtitles_label)
                    is Setting.Speed -> context.getString(R.string.player_settings_speed_label)
                    else -> ""
                }
            }

            binding.tvSettingSubText.apply {
                text = when (setting) {
                    is Setting.Quality -> when (val selected = setting.selected) {
                        is Quality.Auto -> when (val track = selected.currentTrack) {
                            null -> context.getString(R.string.player_settings_quality_auto)
                            else -> context.getString(
                                R.string.player_settings_quality_auto_selected,
                                track.height
                            )
                        }
                        is Quality.VideoTrackInformation -> context.getString(
                            R.string.player_settings_quality,
                            selected.height
                        )
                    }
                    is Setting.Subtitle -> when (val selected = setting.selected) {
                        is Subtitle.None -> context.getString(R.string.player_settings_subtitles_off)
                        is Subtitle.TextTrackInformation -> selected.name
                    }
                    is Setting.Speed -> context.getString(setting.selected.stringId)
                    else -> ""
                }
                visibility = when {
                    text.isEmpty() -> View.GONE
                    else -> View.VISIBLE
                }
            }

            binding.ivSettingEnter.visibility = View.VISIBLE

            binding.ivSettingIsSelected.visibility = View.GONE
        }

        fun displayQualitySetting(quality: Quality) {
            binding.root.setOnClickListener {
                playerSettingsView.player?.let { player ->
                    when (quality) {
                        is Quality.Auto -> {
                            player.trackSelectionParameters = player.trackSelectionParameters
                                .buildUpon()
                                .setMaxVideoBitrate(Int.MAX_VALUE)
                                .setForceHighestSupportedBitrate(false)
                                .build()
                        }
                        is Quality.VideoTrackInformation -> {
                            player.trackSelectionParameters = player.trackSelectionParameters
                                .buildUpon()
                                .setMaxVideoBitrate(quality.bitrate)
                                .setForceHighestSupportedBitrate(true)
                                .build()
                        }
                    }
                }
                playerSettingsView.hide()
            }

            binding.ivSettingIcon.visibility = View.GONE

            binding.tvSettingMainText.apply {

                text = when (quality) {
                    is Quality.Auto -> when {
                        quality.isSelected -> when (val track = quality.currentTrack) {
                            null -> context.getString(R.string.player_settings_quality_auto)
                            else -> context.getString(
                                R.string.player_settings_quality_auto_selected,
                                track.height
                            )
                        }
                        else -> context.getString(R.string.player_settings_quality_auto)
                    }
                    is Quality.VideoTrackInformation -> context.getString(
                        R.string.player_settings_quality,
                        quality.height
                    )
                }
            }

            binding.tvSettingSubText.visibility = View.GONE


            binding.ivSettingEnter.visibility = View.GONE

            binding.ivSettingIsSelected.visibility = when {
                quality.isSelected -> View.VISIBLE
                else -> View.GONE
            }
        }

        fun displaySubtitleSetting(subtitle: Subtitle) {
            binding.root.setOnClickListener {
                playerSettingsView.player?.let { player ->
                    when (subtitle) {
                        is Subtitle.None -> {
                            player.trackSelectionParameters = player.trackSelectionParameters
                                .buildUpon()
                                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                                .setIgnoredTextSelectionFlags(C.SELECTION_FLAG_FORCED.inv())
                                .build()
                        }
                        is Subtitle.TextTrackInformation -> {
                            player.trackSelectionParameters = player.trackSelectionParameters
                                .buildUpon()
                                .setOverrideForType(
                                    TrackSelectionOverride(
                                        subtitle.trackGroup.mediaTrackGroup,
                                        listOf(subtitle.trackIndex)
                                    )
                                )
                                .setTrackTypeDisabled(subtitle.trackGroup.type, false)
                                .build()
                        }
                    }

                }
                playerSettingsView.hide()
            }

            binding.ivSettingIcon.visibility = View.GONE

            binding.tvSettingMainText.apply {
                text = when (subtitle) {
                    is Subtitle.None -> context.getString(R.string.player_settings_subtitles_off)
                    is Subtitle.TextTrackInformation -> subtitle.name
                }
            }

            binding.tvSettingSubText.visibility = View.GONE

            binding.ivSettingEnter.visibility = View.GONE

            binding.ivSettingIsSelected.visibility = when {
                subtitle.isSelected -> View.VISIBLE
                else -> View.GONE
            }
        }

        fun displaySpeedSetting(playbackSpeed: PlaybackSpeed) {
            binding.root.setOnClickListener {
                playerSettingsView.player?.let { player ->
                    player.playbackParameters = player.playbackParameters
                        .withSpeed(playbackSpeed.speed)
                }
                playerSettingsView.hide()
            }

            binding.ivSettingIcon.visibility = View.GONE

            binding.tvSettingMainText.apply {
                text = context.getString(playbackSpeed.stringId)
            }

            binding.tvSettingSubText.visibility = View.GONE

            binding.ivSettingEnter.visibility = View.GONE

            binding.ivSettingIsSelected.visibility = when {
                playbackSpeed.isSelected -> View.VISIBLE
                else -> View.GONE
            }
        }
    }


    private sealed class Quality {

        abstract val isSelected: Boolean

        object Auto : Quality() {
            val currentTrack: VideoTrackInformation?
                get() = Setting.Quality.list
                    .filterIsInstance<VideoTrackInformation>()
                    .find { it.isCurrentlyPlayed }
            override val isSelected: Boolean
                get() = Setting.Quality.list
                    .filterIsInstance<VideoTrackInformation>()
                    .none { it.isSelected }
        }

        class VideoTrackInformation(
            val name: String,
            val width: Int,
            val height: Int,
            val bitrate: Int,

            val player: ExoPlayer,
        ) : Quality() {
            val isCurrentlyPlayed: Boolean
                get() = player.videoFormat?.let { it.bitrate == bitrate } ?: false
            override val isSelected: Boolean
                get() = player.trackSelectionParameters.maxVideoBitrate == bitrate
        }
    }

    private sealed class Subtitle {

        abstract val isSelected: Boolean

        object None : Subtitle() {
            override val isSelected: Boolean
                get() = Setting.Subtitle.list
                    .filterIsInstance<TextTrackInformation>()
                    .none { it.isSelected }
        }

        class TextTrackInformation(
            val name: String,

            val trackGroup: Tracks.Group,
            val trackIndex: Int,
        ) : Subtitle() {
            override val isSelected: Boolean
                get() = trackGroup.isTrackSelected(trackIndex)
        }
    }

    class PlaybackSpeed(
        val stringId: Int,
        val speed: Float,
    ) {
        var isSelected: Boolean = false
    }
}