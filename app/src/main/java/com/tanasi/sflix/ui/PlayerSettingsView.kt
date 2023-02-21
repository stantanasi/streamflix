package com.tanasi.sflix.ui

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
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
import com.tanasi.sflix.R
import com.tanasi.sflix.databinding.ItemSettingBinding
import com.tanasi.sflix.databinding.ViewPlayerSettingsBinding
import com.tanasi.sflix.utils.findClosest
import com.tanasi.sflix.utils.trackFormats

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
                        Setting.Speed.updateSelected(value)
                    }
                }
            })
            value?.let { Setting.Speed.updateSelected(it) }

            field = value
        }


    init {
        Setting.Main.adapter.playerSettingsView = this
        Setting.Quality.adapter.playerSettingsView = this
        Setting.Subtitle.adapter.playerSettingsView = this
        Setting.Speed.adapter.playerSettingsView = this
    }

    fun onBackPressed() {
        when (binding.rvSettings.adapter) {
            is SettingsAdapter -> hide()
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
                Setting.Main -> context.getString(R.string.player_settings_title)
                Setting.Quality -> context.getString(R.string.player_settings_quality_title)
                Setting.Subtitle -> context.getString(R.string.player_settings_subtitles_title)
                Setting.Speed -> context.getString(R.string.player_settings_speed_title)
            }
        }

        binding.rvSettings.adapter = when (setting) {
            Setting.Main -> Setting.Main.adapter
            Setting.Quality -> Setting.Quality.adapter
            Setting.Subtitle -> Setting.Subtitle.adapter
            Setting.Speed -> Setting.Speed.adapter
        }
        binding.rvSettings.requestFocus()
    }

    fun hide() {
        this.visibility = View.GONE
    }


    private sealed class Setting {

        object Main : Setting() {
            val adapter = SettingsAdapter(listOf(Quality, Subtitle, Speed))
        }

        object Quality : Setting() {
            private val list = mutableListOf<VideoTrackInformation>()
            val adapter = VideoTrackSelectionAdapter(list)

            val selected: VideoTrackInformation?
                get() = list.find { it.isSelected }

            fun init(player: ExoPlayer, resources: Resources) {
                list.clear()
                list.addAll(
                    player.currentTracks.groups
                        .filter { it.type == C.TRACK_TYPE_VIDEO }
                        .flatMap { trackGroup ->
                            trackGroup.trackFormats
                                .filter { it.selectionFlags and C.SELECTION_FLAG_FORCED == 0 }
                                .mapIndexed { trackIndex, trackFormat ->
                                    VideoTrackInformation(
                                        name = DefaultTrackNameProvider(resources)
                                            .getTrackName(trackFormat),
                                        width = trackFormat.width,
                                        height = trackFormat.height,
                                        bitrate = trackFormat.bitrate,

                                        player = player,
                                        trackGroup = trackGroup,
                                        trackIndex = trackIndex,
                                    )
                                }
                        }
                )
            }
        }

        object Subtitle : Setting() {
            private val list = mutableListOf<TextTrackInformation>()
            val adapter = TextTrackSelectionAdapter(list)

            val selected: TextTrackInformation?
                get() = list.find { it.isSelected }

            fun init(player: ExoPlayer, resources: Resources) {
                list.clear()
                list.addAll(
                    player.currentTracks.groups
                        .filter { it.type == C.TRACK_TYPE_TEXT }
                        .flatMap { trackGroup ->
                            trackGroup.trackFormats
                                .filter { it.selectionFlags and C.SELECTION_FLAG_FORCED == 0 }
                                .filter { it.label != null }
                                .mapIndexed { trackIndex, trackFormat ->
                                    TextTrackInformation(
                                        name = DefaultTrackNameProvider(resources)
                                            .getTrackName(trackFormat),

                                        trackGroup = trackGroup,
                                        trackIndex = trackIndex,
                                    )
                                }
                        }
                )
            }
        }

        object Speed : Setting() {
            private val list = listOf(
                PlaybackSpeed(R.string.player_settings_speed_0_25, 0.25F),
                PlaybackSpeed(R.string.player_settings_speed_0_5, 0.5F),
                PlaybackSpeed(R.string.player_settings_speed_0_75, 0.75F),
                PlaybackSpeed(R.string.player_settings_speed_1, 1F),
                PlaybackSpeed(R.string.player_settings_speed_1_25, 1.25F),
                PlaybackSpeed(R.string.player_settings_speed_1_5, 1.5F),
                PlaybackSpeed(R.string.player_settings_speed_1_75, 1.75F),
                PlaybackSpeed(R.string.player_settings_speed_2, 2F),
            )
            val adapter = PlaybackSpeedAdapter(list)

            val selected: PlaybackSpeed?
                get() = list.find { it.isSelected }

            fun updateSelected(player: ExoPlayer) {
                list.forEach { it.isSelected = false }
                list.findClosest(player.playbackParameters.speed) { it.speed }?.isSelected = true
            }
        }
    }


    private class SettingsAdapter(
        private val settings: List<Setting>,
    ) : RecyclerView.Adapter<SettingViewHolder>() {

        lateinit var playerSettingsView: PlayerSettingsView

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingViewHolder {
            return SettingViewHolder(
                ItemSettingBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: SettingViewHolder, position: Int) {
            val setting = settings[position]

            holder.binding.root.setOnClickListener {
                playerSettingsView.displaySetting(setting)
            }

            holder.binding.ivSettingIcon.apply {
                when (setting) {
                    Setting.Quality -> setImageDrawable(
                        ContextCompat.getDrawable(context, R.drawable.ic_settings_quality)
                    )
                    Setting.Subtitle -> setImageDrawable(
                        ContextCompat.getDrawable(
                            context, when (Setting.Subtitle.selected) {
                                null -> R.drawable.ic_settings_subtitle_off
                                else -> R.drawable.ic_settings_subtitle_on
                            }
                        )
                    )
                    Setting.Speed -> setImageDrawable(
                        ContextCompat.getDrawable(context, R.drawable.ic_settings_playback_speed)
                    )
                    else -> {}
                }
                visibility = View.VISIBLE
            }

            holder.binding.tvSettingMainText.apply {
                text = when (setting) {
                    Setting.Quality -> context.getString(R.string.player_settings_quality_label)
                    Setting.Subtitle -> context.getString(R.string.player_settings_subtitles_label)
                    Setting.Speed -> context.getString(R.string.player_settings_speed_label)
                    else -> ""
                }
            }

            holder.binding.tvSettingSubText.apply {
                text = when (setting) {
                    Setting.Quality -> Setting.Quality.selected?.let {
                        when (Setting.Quality.adapter.selectedIndex) {
                            0 -> context.getString(
                                R.string.player_settings_quality_sub_label_auto,
                                it.height
                            )
                            else -> context.getString(R.string.player_settings_quality, it.height)
                        }
                    } ?: ""
                    Setting.Subtitle -> Setting.Subtitle.selected?.name
                        ?: context.getString(R.string.player_settings_subtitles_off)
                    Setting.Speed -> Setting.Speed.selected?.let {
                        context.getString(it.stringId)
                    } ?: ""
                    else -> ""
                }
                visibility = when {
                    text.isEmpty() -> View.GONE
                    else -> View.VISIBLE
                }
            }

            holder.binding.ivSettingCheck.apply {
                setColorFilter(Color.parseColor("#808080"))
                setImageDrawable(
                    ContextCompat.getDrawable(context, R.drawable.ic_setting_arrow_right)
                )
                visibility = View.VISIBLE
            }
        }

        override fun getItemCount() = settings.size
    }


    private class VideoTrackInformation(
        val name: String,
        val width: Int,
        val height: Int,
        val bitrate: Int,

        val player: ExoPlayer,
        val trackGroup: Tracks.Group,
        val trackIndex: Int,
    ) {
        val isSelected: Boolean
            get() = player.videoFormat?.bitrate == bitrate && trackGroup.isTrackSelected(trackIndex)
    }

    private class VideoTrackSelectionAdapter(
        private val tracks: List<VideoTrackInformation>,
    ) : RecyclerView.Adapter<SettingViewHolder>() {

        lateinit var playerSettingsView: PlayerSettingsView
        var selectedIndex = 0

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingViewHolder {
            return SettingViewHolder(
                ItemSettingBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: SettingViewHolder, position: Int) {
            if (position == 0) {
                holder.binding.root.setOnClickListener {
                    selectedIndex = 0
                    playerSettingsView.player?.let { player ->
                        player.trackSelectionParameters = player.trackSelectionParameters
                            .buildUpon()
                            .setMaxVideoBitrate(Int.MAX_VALUE)
                            .setForceHighestSupportedBitrate(false)
                            .build()
                    }
                    playerSettingsView.hide()
                }

                holder.binding.ivSettingIcon.visibility = View.GONE

                holder.binding.tvSettingMainText.apply {
                    text = when (selectedIndex) {
                        0 -> Setting.Quality.selected?.let {
                            context.getString(
                                R.string.player_settings_quality_auto_selected,
                                it.height
                            )
                        } ?: context.getString(R.string.player_settings_quality_auto)
                        else -> context.getString(R.string.player_settings_quality_auto)
                    }
                }

                holder.binding.tvSettingSubText.visibility = View.GONE

                holder.binding.ivSettingCheck.visibility = when (selectedIndex) {
                    0 -> View.VISIBLE
                    else -> View.GONE
                }
            } else {
                val track = tracks[position - 1]

                holder.binding.root.setOnClickListener {
                    selectedIndex = holder.bindingAdapterPosition
                    playerSettingsView.player?.let { player ->
                        player.trackSelectionParameters = player.trackSelectionParameters
                            .buildUpon()
                            .setMaxVideoBitrate(track.bitrate)
                            .setForceHighestSupportedBitrate(true)
                            .build()
                    }
                    playerSettingsView.hide()
                }

                holder.binding.ivSettingIcon.visibility = View.GONE

                holder.binding.tvSettingMainText.apply {
                    text = context.getString(R.string.player_settings_quality, track.height)
                }

                holder.binding.tvSettingSubText.visibility = View.GONE

                holder.binding.ivSettingCheck.visibility = when (position) {
                    selectedIndex -> View.VISIBLE
                    else -> View.GONE
                }
            }
        }

        override fun getItemCount() = tracks.size + 1
    }


    private class TextTrackInformation(
        val name: String,

        val trackGroup: Tracks.Group,
        val trackIndex: Int,
    ) {
        val isSelected: Boolean
            get() = trackGroup.isTrackSelected(trackIndex)
    }

    private class TextTrackSelectionAdapter(
        private val tracks: List<TextTrackInformation>,
    ) : RecyclerView.Adapter<SettingViewHolder>() {

        lateinit var playerSettingsView: PlayerSettingsView

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingViewHolder {
            return SettingViewHolder(
                ItemSettingBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: SettingViewHolder, position: Int) {
            if (position == 0) {
                holder.binding.root.setOnClickListener {
                    playerSettingsView.player?.let { player ->
                        player.trackSelectionParameters = player.trackSelectionParameters
                            .buildUpon()
                            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                            .setIgnoredTextSelectionFlags(C.SELECTION_FLAG_FORCED.inv())
                            .build()
                    }
                    playerSettingsView.hide()
                }

                holder.binding.ivSettingIcon.visibility = View.GONE

                holder.binding.tvSettingMainText.apply {
                    text = context.getString(R.string.player_settings_subtitles_off)
                }

                holder.binding.tvSettingSubText.visibility = View.GONE

                holder.binding.ivSettingCheck.visibility = when (Setting.Subtitle.selected) {
                    null -> View.VISIBLE
                    else -> View.GONE
                }
            } else {
                val track = tracks[position - 1]

                holder.binding.root.setOnClickListener {
                    playerSettingsView.player?.let { player ->
                        player.trackSelectionParameters = player.trackSelectionParameters
                            .buildUpon()
                            .setOverrideForType(
                                TrackSelectionOverride(
                                    track.trackGroup.mediaTrackGroup,
                                    listOf(track.trackIndex)
                                )
                            )
                            .setTrackTypeDisabled(track.trackGroup.type, false)
                            .build()
                    }
                    playerSettingsView.hide()
                }

                holder.binding.ivSettingIcon.visibility = View.GONE

                holder.binding.tvSettingMainText.text = track.name

                holder.binding.tvSettingSubText.visibility = View.GONE

                holder.binding.ivSettingCheck.visibility = when {
                    track.isSelected -> View.VISIBLE
                    else -> View.GONE
                }
            }
        }

        override fun getItemCount() = tracks.size + 1
    }


    private class PlaybackSpeed(
        val stringId: Int,
        val speed: Float,
    ) {
        var isSelected: Boolean = false
    }

    private class PlaybackSpeedAdapter(
        private val playbackSpeeds: List<PlaybackSpeed>,
    ) : RecyclerView.Adapter<SettingViewHolder?>() {

        lateinit var playerSettingsView: PlayerSettingsView

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingViewHolder {
            return SettingViewHolder(
                ItemSettingBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: SettingViewHolder, position: Int) {
            val playbackSpeed = playbackSpeeds[position]

            holder.binding.root.setOnClickListener {
                playerSettingsView.player?.let { player ->
                    player.playbackParameters = player.playbackParameters
                        .withSpeed(playbackSpeed.speed)
                }
                playerSettingsView.hide()
            }

            holder.binding.ivSettingIcon.visibility = View.GONE

            holder.binding.tvSettingMainText.apply {
                text = context.getString(playbackSpeed.stringId)
            }

            holder.binding.tvSettingSubText.visibility = View.GONE

            holder.binding.ivSettingCheck.visibility = when {
                playbackSpeed.isSelected -> View.VISIBLE
                else -> View.GONE
            }
        }

        override fun getItemCount() = playbackSpeeds.size
    }


    private class SettingViewHolder(
        val binding: ItemSettingBinding,
    ) : RecyclerView.ViewHolder(binding.root)
}