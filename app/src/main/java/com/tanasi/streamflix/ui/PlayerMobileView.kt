package com.tanasi.streamflix.ui

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.media3.common.Player
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.PlayerView

class PlayerMobileView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : PlayerView(context, attrs, defStyle) {

    val controller: PlayerControlView
        get() = PlayerView::class.java.getDeclaredField("controller").let {
            it.isAccessible = true
            it.get(this) as PlayerControlView
        }
}