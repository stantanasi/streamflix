package com.tanasi.streamflix.ui

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
import com.google.android.exoplayer2.ui.CaptionStyleCompat
import com.google.android.exoplayer2.ui.DefaultTrackNameProvider
import com.google.android.exoplayer2.ui.SubtitleView
import com.tanasi.streamflix.R
import com.tanasi.streamflix.databinding.ItemSettingBinding
import com.tanasi.streamflix.databinding.ViewPlayerSettingsBinding
import com.tanasi.streamflix.utils.UserPreferences
import com.tanasi.streamflix.utils.findClosest
import com.tanasi.streamflix.utils.margin
import com.tanasi.streamflix.utils.setAlpha
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
    var subtitleView: SubtitleView? = null

    private var currentSettings = Setting.MAIN

    private val settingsAdapter = SettingsAdapter(this, Settings.list)
    private val qualityAdapter = SettingsAdapter(this, Settings.Quality.list)
    private val subtitlesAdapter = SettingsAdapter(this, Settings.Subtitle.list)
    private val captionStyleAdapter = SettingsAdapter(this, Settings.Subtitle.Style.list)
    private val speedAdapter = SettingsAdapter(this, Settings.Speed.list)

    fun onBackPressed() {
        when (currentSettings) {
            Setting.MAIN -> hide()
            Setting.QUALITY,
            Setting.SUBTITLES,
            Setting.SPEED -> displaySetting(Setting.MAIN)
            Setting.CAPTION_STYLE -> displaySetting(Setting.SUBTITLES)
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

        displaySetting(Setting.MAIN)
    }

    private fun displaySetting(setting: Setting) {
        currentSettings = setting

        binding.tvSettingsHeader.apply {
            text = when (setting) {
                Setting.MAIN -> context.getString(R.string.player_settings_title)
                Setting.QUALITY -> context.getString(R.string.player_settings_quality_title)
                Setting.SUBTITLES -> context.getString(R.string.player_settings_subtitles_title)
                Setting.CAPTION_STYLE -> context.getString(R.string.player_settings_caption_style_title)
                Setting.SPEED -> context.getString(R.string.player_settings_speed_title)
            }
        }

        binding.rvSettings.adapter = when (setting) {
            Setting.MAIN -> settingsAdapter
            Setting.QUALITY -> qualityAdapter
            Setting.SUBTITLES -> subtitlesAdapter
            Setting.CAPTION_STYLE -> captionStyleAdapter
            Setting.SPEED -> speedAdapter
        }
        binding.rvSettings.requestFocus()
    }

    fun hide() {
        this.visibility = View.GONE
    }


    private enum class Setting {
        MAIN,
        QUALITY,
        SUBTITLES,
        CAPTION_STYLE,
        SPEED
    }


    private class SettingsAdapter(
        private val settingsView: PlayerSettingsView,
        private val items: List<Item>,
    ) : RecyclerView.Adapter<SettingViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            SettingViewHolder(
                settingsView,
                ItemSettingBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        override fun onBindViewHolder(holder: SettingViewHolder, position: Int) {
            holder.displaySettings(items[position])
        }

        override fun getItemCount() = items.size
    }

    private class SettingViewHolder(
        private val settingsView: PlayerSettingsView,
        val binding: ItemSettingBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun displaySettings(item: Item) {
            val player = settingsView.player ?: return
            val subtitleView = settingsView.subtitleView ?: return

            binding.root.apply {
                when (item) {
                    Settings.Subtitle.Style,
                    Settings.Subtitle.Style.ResetStyle -> margin(bottom = 16F)
                }
                setOnClickListener {
                    when (item) {
                        is Settings -> {
                            when (item) {
                                Settings.Quality -> settingsView.displaySetting(Setting.QUALITY)
                                Settings.Subtitle -> settingsView.displaySetting(Setting.SUBTITLES)
                                Settings.Speed -> settingsView.displaySetting(Setting.SPEED)
                            }
                        }

                        is Settings.Quality -> {
                            when (item) {
                                is Settings.Quality.Auto -> {
                                    player.trackSelectionParameters = player.trackSelectionParameters
                                        .buildUpon()
                                        .setMaxVideoBitrate(Int.MAX_VALUE)
                                        .setForceHighestSupportedBitrate(false)
                                        .build()
                                    settingsView.hide()
                                }
                                is Settings.Quality.VideoTrackInformation -> {
                                    player.trackSelectionParameters = player.trackSelectionParameters
                                        .buildUpon()
                                        .setMaxVideoBitrate(item.bitrate)
                                        .setForceHighestSupportedBitrate(true)
                                        .build()
                                    settingsView.hide()
                                }
                            }
                        }

                        is Settings.Subtitle -> {
                            when (item) {
                                Settings.Subtitle.Style -> {
                                    settingsView.displaySetting(Setting.CAPTION_STYLE)
                                }
                                is Settings.Subtitle.None -> {
                                    player.trackSelectionParameters = player.trackSelectionParameters
                                        .buildUpon()
                                        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                                        .setIgnoredTextSelectionFlags(C.SELECTION_FLAG_FORCED.inv())
                                        .build()
                                    settingsView.hide()
                                }
                                is Settings.Subtitle.TextTrackInformation -> {
                                    player.trackSelectionParameters = player.trackSelectionParameters
                                        .buildUpon()
                                        .setOverrideForType(
                                            TrackSelectionOverride(
                                                item.trackGroup.mediaTrackGroup,
                                                listOf(item.trackIndex)
                                            )
                                        )
                                        .setTrackTypeDisabled(item.trackGroup.type, false)
                                        .build()
                                    settingsView.hide()
                                }
                            }
                        }

                        is Settings.Subtitle.Style -> {
                            when (item) {
                                Settings.Subtitle.Style.ResetStyle -> {
                                    UserPreferences.captionStyle = Settings.Subtitle.Style.DEFAULT
                                    subtitleView.setStyle(UserPreferences.captionStyle)
                                    settingsView.hide()
                                }
                            }
                        }

                        is Settings.Speed -> {
                            player.playbackParameters = player.playbackParameters
                                .withSpeed(item.value)
                            settingsView.hide()
                        }
                    }
                }
            }

            binding.ivSettingIcon.apply {
                when (item) {
                    is Settings -> {
                        when (item) {
                            Settings.Quality -> setImageDrawable(
                                ContextCompat.getDrawable(context, R.drawable.ic_settings_quality)
                            )
                            Settings.Subtitle -> setImageDrawable(
                                ContextCompat.getDrawable(
                                    context,
                                    when (Settings.Subtitle.selected) {
                                        Settings.Subtitle.Style,
                                        is Settings.Subtitle.None -> R.drawable.ic_settings_subtitle_off
                                        is Settings.Subtitle.TextTrackInformation -> R.drawable.ic_settings_subtitle_on
                                    }
                                )
                            )
                            Settings.Speed -> setImageDrawable(
                                ContextCompat.getDrawable(
                                    context,
                                    R.drawable.ic_settings_playback_speed
                                )
                            )
                        }
                        visibility = View.VISIBLE
                    }

                    else -> {
                        visibility = View.GONE
                    }
                }
            }

            binding.tvSettingMainText.apply {
                text = when (item) {
                    is Settings -> when (item) {
                        Settings.Quality -> context.getString(R.string.player_settings_quality_label)
                        Settings.Subtitle -> context.getString(R.string.player_settings_subtitles_label)
                        Settings.Speed -> context.getString(R.string.player_settings_speed_label)
                    }

                    is Settings.Quality -> when (item) {
                        is Settings.Quality.Auto -> when {
                            item.isSelected -> when (val track = item.currentTrack) {
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
                            item.height
                        )
                    }

                    is Settings.Subtitle -> when (item) {
                        Settings.Subtitle.Style -> context.getString(R.string.player_settings_caption_style_label)
                        is Settings.Subtitle.None -> context.getString(R.string.player_settings_subtitles_off)
                        is Settings.Subtitle.TextTrackInformation -> item.name
                    }

                    is Settings.Subtitle.Style -> when (item) {
                        Settings.Subtitle.Style.ResetStyle -> context.getString(R.string.player_settings_caption_style_reset_style_label)
                    }

                    is Settings.Speed -> context.getString(item.stringId)

                    else -> ""
                }
            }

            binding.tvSettingSubText.apply {
                text = when (item) {
                    is Settings -> when (item) {
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
                            Settings.Subtitle.Style,
                            is Settings.Subtitle.None -> context.getString(R.string.player_settings_subtitles_off)
                            is Settings.Subtitle.TextTrackInformation -> selected.name
                        }
                        Settings.Speed -> context.getString(Settings.Speed.selected.stringId)
                    }

                    is Settings.Subtitle -> when (item) {
                        Settings.Subtitle.Style -> context.getString(R.string.player_settings_caption_style_sub_label)
                        else -> ""
                    }

                    is Settings.Subtitle.Style -> when (item) {
                        Settings.Subtitle.Style.ResetStyle -> ""
                    }

                    else -> ""
                }
                visibility = when {
                    text.isEmpty() -> View.GONE
                    else -> View.VISIBLE
                }
            }

            binding.ivSettingIsSelected.apply {
                visibility = when (item) {
                    is Settings.Quality -> when {
                        item.isSelected -> View.VISIBLE
                        else -> View.GONE
                    }

                    is Settings.Subtitle -> when (item) {
                        Settings.Subtitle.Style -> View.GONE
                        is Settings.Subtitle.None -> when {
                            item.isSelected -> View.VISIBLE
                            else -> View.GONE
                        }
                        is Settings.Subtitle.TextTrackInformation -> when {
                            item.isSelected -> View.VISIBLE
                            else -> View.GONE
                        }
                    }

                    is Settings.Speed -> when {
                        item.isSelected -> View.VISIBLE
                        else -> View.GONE
                    }

                    else -> View.GONE
                }
            }

            binding.ivSettingEnter.apply {
                visibility = when (item) {
                    is Settings -> View.VISIBLE

                    is Settings.Subtitle -> when (item) {
                        Settings.Subtitle.Style -> View.VISIBLE
                        is Settings.Subtitle.None -> View.GONE
                        is Settings.Subtitle.TextTrackInformation -> View.GONE
                    }

                    is Settings.Subtitle.Style -> when (item) {
                        Settings.Subtitle.Style.ResetStyle -> View.GONE
                    }

                    else -> View.GONE
                }
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
                    get() = list.find {
                        when (it) {
                            is None -> it.isSelected
                            is TextTrackInformation -> it.isSelected
                            else -> false
                        }
                    } ?: None

                fun init(player: ExoPlayer, resources: Resources) {
                    list.clear()
                    list.add(Style)
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

            sealed class Style : Item {

                companion object : Subtitle() {
                    val DEFAULT = CaptionStyleCompat(
                        Color.WHITE,
                        Color.BLACK.setAlpha(128),
                        Color.TRANSPARENT,
                        CaptionStyleCompat.EDGE_TYPE_NONE,
                        Color.BLACK,
                        null
                    )

                    val list = listOf<Style>(
                        ResetStyle,
                    )
                }

                object ResetStyle : Style()
            }

            object None : Subtitle() {
                val isSelected: Boolean
                    get() = list
                        .filterIsInstance<TextTrackInformation>()
                        .none { it.isSelected }
            }

            class TextTrackInformation(
                val name: String,

                val trackGroup: Tracks.Group,
                val trackIndex: Int,
            ) : Subtitle() {
                val isSelected: Boolean
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