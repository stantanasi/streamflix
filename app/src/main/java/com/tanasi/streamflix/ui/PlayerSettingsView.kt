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
                        Settings.Quality.init(value, resources)
                        Settings.Subtitle.init(value, resources)
                    }
                    if (events.contains(Player.EVENT_PLAYBACK_PARAMETERS_CHANGED)) {
                        Settings.Speed.refresh(value)
                    }
                }
            })
            value?.let { Settings.Speed.refresh(it) }

            field = value
        }

    private val settingsAdapter = SettingsAdapter(Setting.Main).also { it.playerSettingsView = this }
    private val qualityAdapter = SettingsAdapter(Setting.Quality).also { it.playerSettingsView = this }
    private val subtitlesAdapter = SettingsAdapter(Setting.Subtitle).also { it.playerSettingsView = this }
    private val speedAdapter = SettingsAdapter(Setting.Speed).also { it.playerSettingsView = this }

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

        binding.rvSettings.adapter = when (setting) {
            is Setting.Main -> settingsAdapter
            is Setting.Quality -> qualityAdapter
            is Setting.Subtitle -> subtitlesAdapter
            is Setting.Speed -> speedAdapter
        }
        binding.rvSettings.requestFocus()
    }

    fun hide() {
        this.visibility = View.GONE
    }


    private sealed class Setting {

        object Main : Setting()

        object Quality : Setting()

        object Subtitle : Setting()

        object Speed : Setting()
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
                is Setting.Main -> holder.displayMainSetting(Settings.list[position])
                is Setting.Quality -> holder.displayQualitySetting(Settings.Quality.list[position])
                is Setting.Subtitle -> holder.displaySubtitleSetting(Settings.Subtitle.list[position])
                is Setting.Speed -> holder.displaySpeedSetting(Settings.Speed.list[position])
            }
        }

        override fun getItemCount() = when (setting) {
            is Setting.Main -> Settings.list.size
            is Setting.Quality -> Settings.Quality.list.size
            is Setting.Subtitle -> Settings.Subtitle.list.size
            is Setting.Speed -> Settings.Speed.list.size
        }
    }

    private class SettingViewHolder(
        val binding: ItemSettingBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        lateinit var playerSettingsView: PlayerSettingsView

        fun displayMainSetting(settings: Settings) {
            binding.root.setOnClickListener {
                playerSettingsView.displaySetting(
                    when (settings) {
                        Settings.Quality -> Setting.Quality
                        Settings.Subtitle -> Setting.Subtitle
                        Settings.Speed -> Setting.Speed
                    }
                )
            }

            binding.ivSettingIcon.apply {
                when (settings) {
                    Settings.Quality -> setImageDrawable(
                        ContextCompat.getDrawable(context, R.drawable.ic_settings_quality)
                    )
                    Settings.Subtitle -> setImageDrawable(
                        ContextCompat.getDrawable(
                            context,
                            when (Settings.Subtitle.selected) {
                                is Settings.Subtitle.None -> R.drawable.ic_settings_subtitle_off
                                is Settings.Subtitle.TextTrackInformation -> R.drawable.ic_settings_subtitle_on
                            }
                        )
                    )
                    Settings.Speed -> setImageDrawable(
                        ContextCompat.getDrawable(context, R.drawable.ic_settings_playback_speed)
                    )
                }
                visibility = View.VISIBLE
            }

            binding.tvSettingMainText.apply {
                text = when (settings) {
                    Settings.Quality -> context.getString(R.string.player_settings_quality_label)
                    Settings.Subtitle -> context.getString(R.string.player_settings_subtitles_label)
                    Settings.Speed -> context.getString(R.string.player_settings_speed_label)
                }
            }

            binding.tvSettingSubText.apply {
                text = when (settings) {
                    Settings.Quality -> when (val selected = Settings.Quality.selected) {
                        is Settings.Quality.Auto -> when (val track = selected.currentTrack) {
                            null -> context.getString(R.string.player_settings_quality_auto)
                            else -> context.getString(
                                R.string.player_settings_quality_auto_selected,
                                track.height
                            )
                        }
                        is Settings.Quality.VideoTrackInformation -> context.getString(
                            R.string.player_settings_quality,
                            selected.height
                        )
                    }
                    Settings.Subtitle -> when (val selected = Settings.Subtitle.selected) {
                        is Settings.Subtitle.None -> context.getString(R.string.player_settings_subtitles_off)
                        is Settings.Subtitle.TextTrackInformation -> selected.name
                    }
                    Settings.Speed -> context.getString(Settings.Speed.selected.stringId)
                }
                visibility = when {
                    text.isEmpty() -> View.GONE
                    else -> View.VISIBLE
                }
            }

            binding.ivSettingEnter.visibility = View.VISIBLE

            binding.ivSettingIsSelected.visibility = View.GONE
        }

        fun displayQualitySetting(quality: Settings.Quality) {
            binding.root.setOnClickListener {
                playerSettingsView.player?.let { player ->
                    when (quality) {
                        is Settings.Quality.Auto -> {
                            player.trackSelectionParameters = player.trackSelectionParameters
                                .buildUpon()
                                .setMaxVideoBitrate(Int.MAX_VALUE)
                                .setForceHighestSupportedBitrate(false)
                                .build()
                        }
                        is Settings.Quality.VideoTrackInformation -> {
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
                    is Settings.Quality.Auto -> when {
                        quality.isSelected -> when (val track = quality.currentTrack) {
                            null -> context.getString(R.string.player_settings_quality_auto)
                            else -> context.getString(
                                R.string.player_settings_quality_auto_selected,
                                track.height
                            )
                        }
                        else -> context.getString(R.string.player_settings_quality_auto)
                    }
                    is Settings.Quality.VideoTrackInformation -> context.getString(
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

        fun displaySubtitleSetting(subtitle: Settings.Subtitle) {
            binding.root.setOnClickListener {
                playerSettingsView.player?.let { player ->
                    when (subtitle) {
                        is Settings.Subtitle.None -> {
                            player.trackSelectionParameters = player.trackSelectionParameters
                                .buildUpon()
                                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                                .setIgnoredTextSelectionFlags(C.SELECTION_FLAG_FORCED.inv())
                                .build()
                        }
                        is Settings.Subtitle.TextTrackInformation -> {
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
                    is Settings.Subtitle.None -> context.getString(R.string.player_settings_subtitles_off)
                    is Settings.Subtitle.TextTrackInformation -> subtitle.name
                }
            }

            binding.tvSettingSubText.visibility = View.GONE

            binding.ivSettingEnter.visibility = View.GONE

            binding.ivSettingIsSelected.visibility = when {
                subtitle.isSelected -> View.VISIBLE
                else -> View.GONE
            }
        }

        fun displaySpeedSetting(playbackSpeed: Settings.Speed) {
            binding.root.setOnClickListener {
                playerSettingsView.player?.let { player ->
                    player.playbackParameters = player.playbackParameters
                        .withSpeed(playbackSpeed.value)
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

    private interface Item

    sealed class Settings : Item {

        companion object {
            val list = listOf(
                Quality,
                Subtitle,
                Speed,
            )
        }

        sealed class Quality : Item {

            companion object : Settings() {
                val list = mutableListOf<Quality>()

                val selected: Quality
                    get() = list.find { it.isSelected } ?: Auto

                fun init(player: ExoPlayer, resources: Resources) {
                    list.clear()
                    list.add(Auto)
                    list.addAll(
                        player.currentTracks.groups
                            .filter { it.type == C.TRACK_TYPE_VIDEO }
                            .flatMap { trackGroup ->
                                trackGroup.trackFormats
                                    .filter { it.selectionFlags and C.SELECTION_FLAG_FORCED == 0 }
                                    .distinctBy { it.width to it.height }
                                    .map { trackFormat ->
                                        VideoTrackInformation(
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

            abstract val isSelected: Boolean

            object Auto : Quality() {
                val currentTrack: VideoTrackInformation?
                    get() = list
                        .filterIsInstance<VideoTrackInformation>()
                        .find { it.isCurrentlyPlayed }
                override val isSelected: Boolean
                    get() = list
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

        sealed class Subtitle : Item {

            companion object : Settings() {
                val list = mutableListOf<Subtitle>()

                val selected: Subtitle
                    get() = list.find { it.isSelected } ?: None

                fun init(player: ExoPlayer, resources: Resources) {
                    list.clear()
                    list.add(None)
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
                            .sortedBy { it.name }
                    )
                }
            }

            abstract val isSelected: Boolean

            object None : Subtitle() {
                override val isSelected: Boolean
                    get() = list
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

        class Speed(
            val stringId: Int,
            val value: Float,
        ) : Item {
            var isSelected: Boolean = false

            companion object : Settings() {
                private val DEFAULT = Speed(R.string.player_settings_speed_1, 1F)

                val list = listOf(
                    Speed(R.string.player_settings_speed_0_25, 0.25F),
                    Speed(R.string.player_settings_speed_0_5, 0.5F),
                    Speed(R.string.player_settings_speed_0_75, 0.75F),
                    DEFAULT,
                    Speed(R.string.player_settings_speed_1_25, 1.25F),
                    Speed(R.string.player_settings_speed_1_5, 1.5F),
                    Speed(R.string.player_settings_speed_1_75, 1.75F),
                    Speed(R.string.player_settings_speed_2, 2F),
                )

                val selected: Speed
                    get() = list.find { it.isSelected }
                        ?: list.find { it.value == 1F }
                        ?: DEFAULT

                fun refresh(player: ExoPlayer) {
                    list.forEach { it.isSelected = false }
                    list.findClosest(player.playbackParameters.speed) { it.value }?.let {
                        it.isSelected = true
                    }
                }
            }
        }
    }
}