package com.tanasi.sflix.fragments.player

import androidx.lifecycle.ViewModel
import com.tanasi.sflix.services.SflixService

class PlayerViewModel : ViewModel() {

    private val sflixService = SflixService.build()
}