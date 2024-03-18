package com.tanasi.streamflix.models

import com.tanasi.streamflix.adapters.AppAdapter

sealed interface Show : AppAdapter.Item {
    var isFavorite: Boolean
}
