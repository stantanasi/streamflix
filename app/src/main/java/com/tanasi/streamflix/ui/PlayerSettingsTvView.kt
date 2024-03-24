package com.tanasi.streamflix.ui

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.DefaultTrackNameProvider
import androidx.media3.ui.SubtitleView
import androidx.recyclerview.widget.RecyclerView
import com.tanasi.streamflix.R
import com.tanasi.streamflix.databinding.ItemSettingTvBinding
import com.tanasi.streamflix.databinding.ViewPlayerSettingsTvBinding
import com.tanasi.streamflix.utils.UserPreferences
import com.tanasi.streamflix.utils.dp
import com.tanasi.streamflix.utils.findClosest
import com.tanasi.streamflix.utils.getAlpha
import com.tanasi.streamflix.utils.getRgb
import com.tanasi.streamflix.utils.margin
import com.tanasi.streamflix.utils.mediaServerId
import com.tanasi.streamflix.utils.mediaServers
import com.tanasi.streamflix.utils.setAlpha
import com.tanasi.streamflix.utils.setRgb
import com.tanasi.streamflix.utils.trackFormats
import kotlin.math.roundToInt

class PlayerSettingsTvView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    val binding = ViewPlayerSettingsTvBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

    var player: ExoPlayer? = null
        set(value) {
            if (field === value) return

            value?.let {
                Settings.Server.init(it)
                Settings.Quality.init(it, resources)
                Settings.Subtitle.init(it, resources)
                Settings.Speed.refresh(it)
            }

            value?.addListener(object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    if (events.contains(Player.EVENT_PLAYLIST_METADATA_CHANGED)) {
                        Settings.Server.init(value)
                    }
                    if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                        Settings.Server.refresh(value)
                    }
                    if (events.contains(Player.EVENT_TRACKS_CHANGED)) {
                        Settings.Quality.init(value, resources)
                        Settings.Subtitle.init(value, resources)
                    }
                    if (events.contains(Player.EVENT_PLAYBACK_PARAMETERS_CHANGED)) {
                        Settings.Speed.refresh(value)
                    }
                }
            })

            field = value
        }
    var subtitleView: SubtitleView? = null

    private var onServerSelected: ((Settings.Server) -> Unit)? = null

    private var currentSettings = Setting.MAIN

    private val settingsAdapter = SettingsAdapter(this, Settings.list)
    private val qualityAdapter = SettingsAdapter(this, Settings.Quality.list)
    private val subtitlesAdapter = SettingsAdapter(this, Settings.Subtitle.list)
    private val captionStyleAdapter = SettingsAdapter(this, Settings.Subtitle.Style.list)
    private val fontColorAdapter = SettingsAdapter(this, Settings.Subtitle.Style.FontColor.list)
    private val textSizeAdapter = SettingsAdapter(this, Settings.Subtitle.Style.TextSize.list)
    private val fontOpacityAdapter = SettingsAdapter(this, Settings.Subtitle.Style.FontOpacity.list)
    private val edgeStyleAdapter = SettingsAdapter(this, Settings.Subtitle.Style.EdgeStyle.list)
    private val backgroundColorAdapter = SettingsAdapter(this, Settings.Subtitle.Style.BackgroundColor.list)
    private val backgroundOpacityAdapter = SettingsAdapter(this, Settings.Subtitle.Style.BackgroundOpacity.list)
    private val windowColorAdapter = SettingsAdapter(this, Settings.Subtitle.Style.WindowColor.list)
    private val windowOpacityAdapter = SettingsAdapter(this, Settings.Subtitle.Style.WindowOpacity.list)
    private val speedAdapter = SettingsAdapter(this, Settings.Speed.list)
    private val serversAdapter = SettingsAdapter(this, Settings.Server.list)

    fun onBackPressed(): Boolean {
        when (currentSettings) {
            Setting.MAIN -> hide()
            Setting.QUALITY,
            Setting.SUBTITLES,
            Setting.SPEED,
            Setting.SERVERS -> displaySettings(Setting.MAIN)
            Setting.CAPTION_STYLE -> displaySettings(Setting.SUBTITLES)
            Setting.CAPTION_STYLE_FONT_COLOR,
            Setting.CAPTION_STYLE_TEXT_SIZE,
            Setting.CAPTION_STYLE_FONT_OPACITY,
            Setting.CAPTION_STYLE_EDGE_STYLE,
            Setting.CAPTION_STYLE_BACKGROUND_COLOR,
            Setting.CAPTION_STYLE_BACKGROUND_OPACITY,
            Setting.CAPTION_STYLE_WINDOW_COLOR,
            Setting.CAPTION_STYLE_WINDOW_OPACITY -> displaySettings(Setting.CAPTION_STYLE)
        }
        return true
    }

    override fun focusSearch(focused: View, direction: Int): View {
        return when {
            binding.rvSettings.hasFocus() -> focused
            else -> super.focusSearch(focused, direction)
        }
    }


    fun show() {
        this.visibility = View.VISIBLE

        displaySettings(Setting.MAIN)
    }

    private fun displaySettings(setting: Setting) {
        currentSettings = setting

        binding.tvSettingsHeader.apply {
            text = when (setting) {
                Setting.MAIN -> context.getString(R.string.player_settings_title)
                Setting.QUALITY -> context.getString(R.string.player_settings_quality_title)
                Setting.SUBTITLES -> context.getString(R.string.player_settings_subtitles_title)
                Setting.CAPTION_STYLE -> context.getString(R.string.player_settings_caption_style_title)
                Setting.CAPTION_STYLE_FONT_COLOR -> context.getString(R.string.player_settings_caption_style_font_color_title)
                Setting.CAPTION_STYLE_TEXT_SIZE -> context.getString(R.string.player_settings_caption_style_text_size_title)
                Setting.CAPTION_STYLE_FONT_OPACITY -> context.getString(R.string.player_settings_caption_style_font_opacity_title)
                Setting.CAPTION_STYLE_EDGE_STYLE -> context.getString(R.string.player_settings_caption_style_edge_style_title)
                Setting.CAPTION_STYLE_BACKGROUND_COLOR -> context.getString(R.string.player_settings_caption_style_background_color_title)
                Setting.CAPTION_STYLE_BACKGROUND_OPACITY -> context.getString(R.string.player_settings_caption_style_background_opacity_title)
                Setting.CAPTION_STYLE_WINDOW_COLOR -> context.getString(R.string.player_settings_caption_style_window_color_title)
                Setting.CAPTION_STYLE_WINDOW_OPACITY -> context.getString(R.string.player_settings_caption_style_window_opacity_title)
                Setting.SPEED -> context.getString(R.string.player_settings_speed_title)
                Setting.SERVERS -> context.getString(R.string.player_settings_servers_title)
            }
        }

        binding.rvSettings.adapter = when (setting) {
            Setting.MAIN -> settingsAdapter
            Setting.QUALITY -> qualityAdapter
            Setting.SUBTITLES -> subtitlesAdapter
            Setting.CAPTION_STYLE -> captionStyleAdapter
            Setting.CAPTION_STYLE_FONT_COLOR -> fontColorAdapter
            Setting.CAPTION_STYLE_TEXT_SIZE -> textSizeAdapter
            Setting.CAPTION_STYLE_FONT_OPACITY -> fontOpacityAdapter
            Setting.CAPTION_STYLE_EDGE_STYLE -> edgeStyleAdapter
            Setting.CAPTION_STYLE_BACKGROUND_COLOR -> backgroundColorAdapter
            Setting.CAPTION_STYLE_BACKGROUND_OPACITY -> backgroundOpacityAdapter
            Setting.CAPTION_STYLE_WINDOW_COLOR -> windowColorAdapter
            Setting.CAPTION_STYLE_WINDOW_OPACITY -> windowOpacityAdapter
            Setting.SPEED -> speedAdapter
            Setting.SERVERS -> serversAdapter
        }
        binding.rvSettings.requestFocus()
    }

    fun hide() {
        this.visibility = View.GONE
    }


    fun setOnServerSelected(onServerSelected: (server: Settings.Server) -> Unit) {
        this.onServerSelected = onServerSelected
    }


    private enum class Setting {
        MAIN,
        QUALITY,
        SUBTITLES,
        CAPTION_STYLE,
        CAPTION_STYLE_FONT_COLOR,
        CAPTION_STYLE_TEXT_SIZE,
        CAPTION_STYLE_FONT_OPACITY,
        CAPTION_STYLE_EDGE_STYLE,
        CAPTION_STYLE_BACKGROUND_COLOR,
        CAPTION_STYLE_BACKGROUND_OPACITY,
        CAPTION_STYLE_WINDOW_COLOR,
        CAPTION_STYLE_WINDOW_OPACITY,
        SPEED,
        SERVERS,
    }


    private class SettingsAdapter(
        private val settingsView: PlayerSettingsTvView,
        private val items: List<Item>,
    ) : RecyclerView.Adapter<SettingViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            SettingViewHolder(
                settingsView,
                ItemSettingTvBinding.inflate(
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
        private val settingsView: PlayerSettingsTvView,
        val binding: ItemSettingTvBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun displaySettings(item: Item) {
            val player = settingsView.player ?: return
            val subtitleView = settingsView.subtitleView ?: return

            binding.root.apply {
                when (item) {
                    Settings.Subtitle.Style,
                    Settings.Subtitle.Style.ResetStyle -> margin(bottom = 16.dp(context))
                    else -> margin(bottom = 0)
                }
                setOnClickListener {
                    when (item) {
                        is Settings -> {
                            when (item) {
                                Settings.Quality -> settingsView.displaySettings(Setting.QUALITY)
                                Settings.Subtitle -> settingsView.displaySettings(Setting.SUBTITLES)
                                Settings.Speed -> settingsView.displaySettings(Setting.SPEED)
                                Settings.Server -> settingsView.displaySettings(Setting.SERVERS)
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
                                    UserPreferences.qualityHeight = null
                                    settingsView.hide()
                                }
                                is Settings.Quality.VideoTrackInformation -> {
                                    player.trackSelectionParameters = player.trackSelectionParameters
                                        .buildUpon()
                                        .setMaxVideoBitrate(item.bitrate)
                                        .setForceHighestSupportedBitrate(true)
                                        .build()
                                    UserPreferences.qualityHeight = item.height
                                    settingsView.hide()
                                }
                            }
                        }

                        is Settings.Subtitle -> {
                            when (item) {
                                Settings.Subtitle.Style -> {
                                    settingsView.displaySettings(Setting.CAPTION_STYLE)
                                }
                                is Settings.Subtitle.None -> {
                                    player.trackSelectionParameters = player.trackSelectionParameters
                                        .buildUpon()
                                        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                                        .setIgnoredTextSelectionFlags(C.SELECTION_FLAG_FORCED.inv())
                                        .build()
                                    UserPreferences.subtitleName = null
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
                                    UserPreferences.subtitleName = item.name
                                    settingsView.hide()
                                }
                            }
                        }

                        is Settings.Subtitle.Style -> {
                            when (item) {
                                Settings.Subtitle.Style.ResetStyle -> {
                                    UserPreferences.also {
                                        it.captionTextSize = Settings.Subtitle.Style.TextSize.DEFAULT.value
                                        it.captionStyle = Settings.Subtitle.Style.DEFAULT
                                    }
                                    subtitleView.also {
                                        it.setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * UserPreferences.captionTextSize)
                                        it.setStyle(UserPreferences.captionStyle)
                                    }
                                    settingsView.hide()
                                }
                                Settings.Subtitle.Style.FontColor -> {
                                    settingsView.displaySettings(Setting.CAPTION_STYLE_FONT_COLOR)
                                }
                                Settings.Subtitle.Style.TextSize -> {
                                    settingsView.displaySettings(Setting.CAPTION_STYLE_TEXT_SIZE)
                                }
                                Settings.Subtitle.Style.FontOpacity -> {
                                    settingsView.displaySettings(Setting.CAPTION_STYLE_FONT_OPACITY)
                                }
                                Settings.Subtitle.Style.EdgeStyle -> {
                                    settingsView.displaySettings(Setting.CAPTION_STYLE_EDGE_STYLE)
                                }
                                Settings.Subtitle.Style.BackgroundColor -> {
                                    settingsView.displaySettings(Setting.CAPTION_STYLE_BACKGROUND_COLOR)
                                }
                                Settings.Subtitle.Style.BackgroundOpacity -> {
                                    settingsView.displaySettings(Setting.CAPTION_STYLE_BACKGROUND_OPACITY)
                                }
                                Settings.Subtitle.Style.WindowColor -> {
                                    settingsView.displaySettings(Setting.CAPTION_STYLE_WINDOW_COLOR)
                                }
                                Settings.Subtitle.Style.WindowOpacity -> {
                                    settingsView.displaySettings(Setting.CAPTION_STYLE_WINDOW_OPACITY)
                                }
                            }
                        }

                        is Settings.Subtitle.Style.FontColor -> {
                            UserPreferences.captionStyle = CaptionStyleCompat(
                                UserPreferences.captionStyle.foregroundColor.setRgb(item.color),
                                UserPreferences.captionStyle.backgroundColor,
                                UserPreferences.captionStyle.windowColor,
                                UserPreferences.captionStyle.edgeType,
                                UserPreferences.captionStyle.edgeColor,
                                null
                            )
                            subtitleView.setStyle(UserPreferences.captionStyle)
                            settingsView.displaySettings(Setting.CAPTION_STYLE)
                        }

                        is Settings.Subtitle.Style.TextSize -> {
                            UserPreferences.captionTextSize = item.value
                            subtitleView.setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * UserPreferences.captionTextSize)
                            settingsView.displaySettings(Setting.CAPTION_STYLE)
                        }

                        is Settings.Subtitle.Style.FontOpacity -> {
                            UserPreferences.captionStyle = CaptionStyleCompat(
                                UserPreferences.captionStyle.foregroundColor.setAlpha(item.alpha),
                                UserPreferences.captionStyle.backgroundColor,
                                UserPreferences.captionStyle.windowColor,
                                UserPreferences.captionStyle.edgeType,
                                UserPreferences.captionStyle.edgeColor,
                                null
                            )
                            subtitleView.setStyle(UserPreferences.captionStyle)
                            settingsView.displaySettings(Setting.CAPTION_STYLE)
                        }

                        is Settings.Subtitle.Style.EdgeStyle -> {
                            UserPreferences.captionStyle = CaptionStyleCompat(
                                UserPreferences.captionStyle.foregroundColor,
                                UserPreferences.captionStyle.backgroundColor,
                                UserPreferences.captionStyle.windowColor,
                                item.type,
                                UserPreferences.captionStyle.edgeColor,
                                null
                            )
                            subtitleView.setStyle(UserPreferences.captionStyle)
                            settingsView.displaySettings(Setting.CAPTION_STYLE)
                        }

                        is Settings.Subtitle.Style.BackgroundColor -> {
                            UserPreferences.captionStyle = CaptionStyleCompat(
                                UserPreferences.captionStyle.foregroundColor,
                                UserPreferences.captionStyle.backgroundColor.setRgb(item.color),
                                UserPreferences.captionStyle.windowColor,
                                UserPreferences.captionStyle.edgeType,
                                UserPreferences.captionStyle.edgeColor,
                                null
                            )
                            subtitleView.setStyle(UserPreferences.captionStyle)
                            settingsView.displaySettings(Setting.CAPTION_STYLE)
                        }

                        is Settings.Subtitle.Style.BackgroundOpacity -> {
                            UserPreferences.captionStyle = CaptionStyleCompat(
                                UserPreferences.captionStyle.foregroundColor,
                                UserPreferences.captionStyle.backgroundColor.setAlpha(item.alpha),
                                UserPreferences.captionStyle.windowColor,
                                UserPreferences.captionStyle.edgeType,
                                UserPreferences.captionStyle.edgeColor,
                                null
                            )
                            subtitleView.setStyle(UserPreferences.captionStyle)
                            settingsView.displaySettings(Setting.CAPTION_STYLE)
                        }

                        is Settings.Subtitle.Style.WindowColor -> {
                            UserPreferences.captionStyle = CaptionStyleCompat(
                                UserPreferences.captionStyle.foregroundColor,
                                UserPreferences.captionStyle.backgroundColor,
                                UserPreferences.captionStyle.windowColor.setRgb(item.color),
                                UserPreferences.captionStyle.edgeType,
                                UserPreferences.captionStyle.edgeColor,
                                null
                            )
                            subtitleView.setStyle(UserPreferences.captionStyle)
                            settingsView.displaySettings(Setting.CAPTION_STYLE)
                        }

                        is Settings.Subtitle.Style.WindowOpacity -> {
                            UserPreferences.captionStyle = CaptionStyleCompat(
                                UserPreferences.captionStyle.foregroundColor,
                                UserPreferences.captionStyle.backgroundColor,
                                UserPreferences.captionStyle.windowColor.setAlpha(item.alpha),
                                UserPreferences.captionStyle.edgeType,
                                UserPreferences.captionStyle.edgeColor,
                                null
                            )
                            subtitleView.setStyle(UserPreferences.captionStyle)
                            settingsView.displaySettings(Setting.CAPTION_STYLE)
                        }

                        is Settings.Speed -> {
                            player.playbackParameters = player.playbackParameters
                                .withSpeed(item.value)
                            settingsView.hide()
                        }

                        is Settings.Server -> {
                            settingsView.onServerSelected?.invoke(item)
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

                            Settings.Server -> setImageDrawable(
                                ContextCompat.getDrawable(
                                    context,
                                    R.drawable.ic_settings_servers
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

            binding.vSettingColor.apply {
                when (item) {
                    is Settings.Subtitle.Style.FontColor -> {
                        backgroundTintList = ColorStateList.valueOf(item.color)
                        visibility = View.VISIBLE
                    }

                    is Settings.Subtitle.Style.BackgroundColor -> {
                        backgroundTintList = ColorStateList.valueOf(item.color)
                        visibility = View.VISIBLE
                    }

                    is Settings.Subtitle.Style.WindowColor -> {
                        backgroundTintList = ColorStateList.valueOf(item.color)
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
                        Settings.Server -> context.getString(R.string.player_settings_servers_label)
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
                        Settings.Subtitle.Style.FontColor -> context.getString(R.string.player_settings_caption_style_font_color_label)
                        Settings.Subtitle.Style.TextSize -> context.getString(R.string.player_settings_caption_style_text_size_label)
                        Settings.Subtitle.Style.FontOpacity -> context.getString(R.string.player_settings_caption_style_font_opacity_label)
                        Settings.Subtitle.Style.EdgeStyle -> context.getString(R.string.player_settings_caption_style_edge_style_label)
                        Settings.Subtitle.Style.BackgroundColor -> context.getString(R.string.player_settings_caption_style_background_color_label)
                        Settings.Subtitle.Style.BackgroundOpacity -> context.getString(R.string.player_settings_caption_style_background_opacity_label)
                        Settings.Subtitle.Style.WindowColor -> context.getString(R.string.player_settings_caption_style_window_color_label)
                        Settings.Subtitle.Style.WindowOpacity -> context.getString(R.string.player_settings_caption_style_window_opacity_label)
                    }

                    is Settings.Subtitle.Style.FontColor -> context.getString(item.stringId)

                    is Settings.Subtitle.Style.TextSize -> context.getString(item.stringId)

                    is Settings.Subtitle.Style.FontOpacity -> context.getString(item.stringId)

                    is Settings.Subtitle.Style.EdgeStyle -> context.getString(item.stringId)

                    is Settings.Subtitle.Style.BackgroundColor -> context.getString(item.stringId)

                    is Settings.Subtitle.Style.BackgroundOpacity -> context.getString(item.stringId)

                    is Settings.Subtitle.Style.WindowColor -> context.getString(item.stringId)

                    is Settings.Subtitle.Style.WindowOpacity -> context.getString(item.stringId)

                    is Settings.Speed -> context.getString(item.stringId)

                    is Settings.Server -> item.name

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
                        Settings.Server -> Settings.Server.selected?.name ?: ""
                    }

                    is Settings.Subtitle -> when (item) {
                        Settings.Subtitle.Style -> context.getString(R.string.player_settings_caption_style_sub_label)
                        else -> ""
                    }

                    is Settings.Subtitle.Style -> when (item) {
                        Settings.Subtitle.Style.ResetStyle -> ""
                        Settings.Subtitle.Style.FontColor -> context.getString(Settings.Subtitle.Style.FontColor.selected.stringId)
                        Settings.Subtitle.Style.TextSize -> context.getString(Settings.Subtitle.Style.TextSize.selected.stringId)
                        Settings.Subtitle.Style.FontOpacity -> context.getString(Settings.Subtitle.Style.FontOpacity.selected.stringId)
                        Settings.Subtitle.Style.EdgeStyle -> context.getString(Settings.Subtitle.Style.EdgeStyle.selected.stringId)
                        Settings.Subtitle.Style.BackgroundColor -> context.getString(Settings.Subtitle.Style.BackgroundColor.selected.stringId)
                        Settings.Subtitle.Style.BackgroundOpacity -> context.getString(Settings.Subtitle.Style.BackgroundOpacity.selected.stringId)
                        Settings.Subtitle.Style.WindowColor -> context.getString(Settings.Subtitle.Style.WindowColor.selected.stringId)
                        Settings.Subtitle.Style.WindowOpacity -> context.getString(Settings.Subtitle.Style.WindowOpacity.selected.stringId)
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

                    is Settings.Subtitle.Style.FontColor -> when {
                        item.isSelected -> View.VISIBLE
                        else -> View.GONE
                    }

                    is Settings.Subtitle.Style.TextSize -> when {
                        item.isSelected -> View.VISIBLE
                        else -> View.GONE
                    }

                    is Settings.Subtitle.Style.FontOpacity -> when {
                        item.isSelected -> View.VISIBLE
                        else -> View.GONE
                    }

                    is Settings.Subtitle.Style.EdgeStyle -> when {
                        item.isSelected -> View.VISIBLE
                        else -> View.GONE
                    }

                    is Settings.Subtitle.Style.BackgroundColor -> when {
                        item.isSelected -> View.VISIBLE
                        else -> View.GONE
                    }

                    is Settings.Subtitle.Style.BackgroundOpacity -> when {
                        item.isSelected -> View.VISIBLE
                        else -> View.GONE
                    }

                    is Settings.Subtitle.Style.WindowColor -> when {
                        item.isSelected -> View.VISIBLE
                        else -> View.GONE
                    }

                    is Settings.Subtitle.Style.WindowOpacity -> when {
                        item.isSelected -> View.VISIBLE
                        else -> View.GONE
                    }

                    is Settings.Speed -> when {
                        item.isSelected -> View.VISIBLE
                        else -> View.GONE
                    }

                    is Settings.Server -> when {
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
                        else -> View.VISIBLE
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
                Server,
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

                    list.filterIsInstance<VideoTrackInformation>()
                        .find { it.height == UserPreferences.qualityHeight }
                        ?.let {
                            player.trackSelectionParameters = player.trackSelectionParameters
                                .buildUpon()
                                .setMaxVideoBitrate(it.bitrate)
                                .setForceHighestSupportedBitrate(true)
                                .build()
                        }
                }
            }

            abstract val isSelected: Boolean

            data object Auto : Quality() {
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

                    list.filterIsInstance<TextTrackInformation>()
                        .find { it.name == UserPreferences.subtitleName }
                        ?.let {
                            player.trackSelectionParameters = player.trackSelectionParameters
                                .buildUpon()
                                .setOverrideForType(
                                    TrackSelectionOverride(
                                        it.trackGroup.mediaTrackGroup,
                                        listOf(it.trackIndex)
                                    )
                                )
                                .setTrackTypeDisabled(it.trackGroup.type, false)
                                .build()
                        }
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

                    val list = listOf(
                        ResetStyle,
                        FontColor,
                        TextSize,
                        FontOpacity,
                        EdgeStyle,
                        BackgroundColor,
                        BackgroundOpacity,
                        WindowColor,
                        WindowOpacity,
                    )
                }

                data object ResetStyle : Style()

                class FontColor(
                    val stringId: Int,
                    val color: Int,
                ) : Item {
                    val isSelected: Boolean
                        get() = color == UserPreferences.captionStyle.foregroundColor.getRgb()

                    companion object : Style() {
                        private val DEFAULT = FontColor(
                            R.string.player_settings_caption_style_font_color_white,
                            Color.WHITE
                        )

                        val list = listOf(
                            DEFAULT,
                            FontColor(
                                R.string.player_settings_caption_style_font_color_yellow,
                                Color.YELLOW
                            ),
                            FontColor(
                                R.string.player_settings_caption_style_font_color_green,
                                Color.GREEN
                            ),
                            FontColor(
                                R.string.player_settings_caption_style_font_color_cyan,
                                Color.CYAN
                            ),
                            FontColor(
                                R.string.player_settings_caption_style_font_color_blue,
                                Color.BLUE
                            ),
                            FontColor(
                                R.string.player_settings_caption_style_font_color_magenta,
                                Color.MAGENTA
                            ),
                            FontColor(
                                R.string.player_settings_caption_style_font_color_red,
                                Color.RED
                            ),
                            FontColor(
                                R.string.player_settings_caption_style_font_color_black,
                                Color.BLACK
                            ),
                        )

                        val selected: FontColor
                            get() = list.find { it.isSelected } ?: DEFAULT
                    }
                }

                class TextSize(
                    val stringId: Int,
                    val value: Float,
                ) : Item {
                    val isSelected: Boolean
                        get() = value == UserPreferences.captionTextSize

                    companion object : Style() {
                        val DEFAULT = TextSize(R.string.player_settings_caption_style_text_size_1, 1F)

                        val list = listOf(
                            TextSize(R.string.player_settings_caption_style_text_size_0_5, 0.5F),
                            TextSize(R.string.player_settings_caption_style_text_size_0_75, 0.75F),
                            DEFAULT,
                            TextSize(R.string.player_settings_caption_style_text_size_2, 2F),
                            TextSize(R.string.player_settings_caption_style_text_size_3, 3F),
                        )

                        val selected: TextSize
                            get() = list.find { it.isSelected } ?: DEFAULT
                    }
                }

                class FontOpacity(
                    val stringId: Int,
                    private val value: Float,
                ) : Item {
                    val alpha: Int
                        get() = (value * 255).roundToInt()

                    val isSelected: Boolean
                        get() = alpha == UserPreferences.captionStyle.foregroundColor.getAlpha()

                    companion object : Style() {
                        private val DEFAULT = FontOpacity(
                            R.string.player_settings_caption_style_font_opacity_1,
                            1F
                        )

                        val list = listOf(
                            FontOpacity(
                                R.string.player_settings_caption_style_font_opacity_0_5,
                                0.5F
                            ),
                            FontOpacity(
                                R.string.player_settings_caption_style_font_opacity_0_75,
                                0.75F
                            ),
                            DEFAULT,
                        )

                        val selected: FontOpacity
                            get() = list.find { it.isSelected } ?: DEFAULT
                    }
                }

                class EdgeStyle(
                    val stringId: Int,
                    val type: Int,
                ) : Item {
                    val isSelected: Boolean
                        get() = type == UserPreferences.captionStyle.edgeType

                    companion object : Style() {
                        private val DEFAULT = EdgeStyle(
                            R.string.player_settings_caption_style_edge_style_none,
                            CaptionStyleCompat.EDGE_TYPE_NONE
                        )

                        val list = listOf(
                            DEFAULT,
                            EdgeStyle(
                                R.string.player_settings_caption_style_edge_style_raised,
                                CaptionStyleCompat.EDGE_TYPE_RAISED
                            ),
                            EdgeStyle(
                                R.string.player_settings_caption_style_edge_style_depressed,
                                CaptionStyleCompat.EDGE_TYPE_DEPRESSED
                            ),
                            EdgeStyle(
                                R.string.player_settings_caption_style_edge_style_outline,
                                CaptionStyleCompat.EDGE_TYPE_OUTLINE
                            ),
                            EdgeStyle(
                                R.string.player_settings_caption_style_edge_style_drop_shadow,
                                CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
                            ),
                        )

                        val selected: EdgeStyle
                            get() = list.find { it.isSelected } ?: DEFAULT
                    }
                }

                class BackgroundColor(
                    val stringId: Int,
                    val color: Int,
                ) : Item {
                    val isSelected: Boolean
                        get() = color == UserPreferences.captionStyle.backgroundColor.getRgb()

                    companion object : Style() {
                        private val DEFAULT = BackgroundColor(
                            R.string.player_settings_caption_style_background_color_black,
                            Color.BLACK
                        )

                        val list = listOf(
                            DEFAULT,
                            BackgroundColor(
                                R.string.player_settings_caption_style_background_color_yellow,
                                Color.YELLOW
                            ),
                            BackgroundColor(
                                R.string.player_settings_caption_style_background_color_green,
                                Color.GREEN
                            ),
                            BackgroundColor(
                                R.string.player_settings_caption_style_background_color_cyan,
                                Color.CYAN
                            ),
                            BackgroundColor(
                                R.string.player_settings_caption_style_background_color_blue,
                                Color.BLUE
                            ),
                            BackgroundColor(
                                R.string.player_settings_caption_style_background_color_magenta,
                                Color.MAGENTA
                            ),
                            BackgroundColor(
                                R.string.player_settings_caption_style_background_color_red,
                                Color.RED
                            ),
                            BackgroundColor(
                                R.string.player_settings_caption_style_background_color_white,
                                Color.WHITE
                            ),
                        )

                        val selected: BackgroundColor
                            get() = list.find { it.isSelected } ?: DEFAULT
                    }
                }

                class BackgroundOpacity(
                    val stringId: Int,
                    private val value: Float,
                ) : Item {
                    val alpha: Int
                        get() = (value * 255).roundToInt()

                    val isSelected: Boolean
                        get() = alpha == UserPreferences.captionStyle.backgroundColor.getAlpha()

                    companion object : Style() {
                        private val DEFAULT = BackgroundOpacity(
                            R.string.player_settings_caption_style_background_opacity_0_5,
                            0.5F
                        )

                        val list = listOf(
                            BackgroundOpacity(
                                R.string.player_settings_caption_style_background_opacity_0,
                                0F
                            ),
                            BackgroundOpacity(
                                R.string.player_settings_caption_style_background_opacity_0_25,
                                0.25F
                            ),
                            DEFAULT,
                            BackgroundOpacity(
                                R.string.player_settings_caption_style_background_opacity_0_75,
                                0.75F
                            ),
                            BackgroundOpacity(
                                R.string.player_settings_caption_style_background_opacity_1,
                                1F
                            ),
                        )

                        val selected: BackgroundOpacity
                            get() = list.find { it.isSelected } ?: DEFAULT
                    }
                }

                class WindowColor(
                    val stringId: Int,
                    val color: Int,
                ) : Item {
                    val isSelected: Boolean
                        get() = color == UserPreferences.captionStyle.windowColor.getRgb()

                    companion object : Style() {
                        private val DEFAULT = WindowColor(
                            R.string.player_settings_caption_style_window_color_black,
                            Color.BLACK
                        )

                        val list = listOf(
                            DEFAULT,
                            WindowColor(
                                R.string.player_settings_caption_style_window_color_yellow,
                                Color.YELLOW
                            ),
                            WindowColor(
                                R.string.player_settings_caption_style_window_color_green,
                                Color.GREEN
                            ),
                            WindowColor(
                                R.string.player_settings_caption_style_window_color_cyan,
                                Color.CYAN
                            ),
                            WindowColor(
                                R.string.player_settings_caption_style_window_color_blue,
                                Color.BLUE
                            ),
                            WindowColor(
                                R.string.player_settings_caption_style_window_color_magenta,
                                Color.MAGENTA
                            ),
                            WindowColor(
                                R.string.player_settings_caption_style_window_color_red,
                                Color.RED
                            ),
                            WindowColor(
                                R.string.player_settings_caption_style_window_color_white,
                                Color.WHITE
                            ),
                        )

                        val selected: WindowColor
                            get() = list.find { it.isSelected } ?: DEFAULT
                    }
                }

                class WindowOpacity(
                    val stringId: Int,
                    private val value: Float,
                ) : Item {
                    val alpha: Int
                        get() = (value * 255).roundToInt()

                    val isSelected: Boolean
                        get() = alpha == UserPreferences.captionStyle.windowColor.getAlpha()

                    companion object : Style() {
                        private val DEFAULT = WindowOpacity(
                            R.string.player_settings_caption_style_window_opacity_0,
                            0F
                        )

                        val list = listOf(
                            DEFAULT,
                            WindowOpacity(
                                R.string.player_settings_caption_style_window_opacity_0_25,
                                0.25F
                            ),
                            WindowOpacity(
                                R.string.player_settings_caption_style_window_opacity_0_5,
                                0.5F
                            ),
                            WindowOpacity(
                                R.string.player_settings_caption_style_window_opacity_0_75,
                                0.75F
                            ),
                            WindowOpacity(
                                R.string.player_settings_caption_style_window_opacity_1,
                                1F
                            ),
                        )

                        val selected: WindowOpacity
                            get() = list.find { it.isSelected } ?: DEFAULT
                    }
                }
            }

            data object None : Subtitle() {
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

        class Server(
            val id: String,
            val name: String,
        ) : Item {
            var isSelected: Boolean = false

            companion object : Settings() {
                val list = mutableListOf<Server>()

                val selected: Server?
                    get() = list.find { it.isSelected }

                fun init(player: ExoPlayer) {
                    list.clear()
                    list.addAll(player.playlistMetadata.mediaServers.map {
                        Server(
                            id = it.id,
                            name = it.name,
                        )
                    })

                    list.firstOrNull()?.isSelected = true
                }

                fun refresh(player: ExoPlayer) {
                    list.forEach {
                        it.isSelected = (it.id == player.mediaMetadata.mediaServerId)
                    }
                }
            }
        }
    }
}