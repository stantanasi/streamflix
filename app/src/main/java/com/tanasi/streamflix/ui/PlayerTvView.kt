package com.tanasi.streamflix.ui

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.media3.common.Player
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.PlayerView

class PlayerTvView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : PlayerView(context, attrs, defStyle) {

    val controller: PlayerControlView
        get() = PlayerView::class.java.getDeclaredField("controller").let {
            it.isAccessible = true
            it.get(this) as PlayerControlView
        }


    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val player = player ?: return super.dispatchKeyEvent(event)

        if (player.isCommandAvailable(Player.COMMAND_GET_CURRENT_MEDIA_ITEM) && player.isPlayingAd) {
            return super.dispatchKeyEvent(event)
        }

        if (controller.isVisible) return super.dispatchKeyEvent(event)

        return when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                player.seekTo(player.currentPosition - 10_000)
                true
            }

            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                player.seekTo(player.currentPosition + 10_000)
                true
            }

            else -> super.dispatchKeyEvent(event)
        }
    }
}