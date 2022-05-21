package com.tanasi.sflix.presenters

import android.view.ViewGroup
import androidx.leanback.widget.Presenter
import androidx.viewbinding.ViewBinding
import com.tanasi.sflix.models.Movie

class MoviePresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
        TODO("Not yet implemented")
    }

    override fun onBindViewHolder(viewHolder: ViewHolder?, item: Any?) {
        TODO("Not yet implemented")
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder?) {
        TODO("Not yet implemented")
    }

    class VhMovie(
        private val _binding: ViewBinding
    ) : ViewHolder(
        _binding.root
    ) {

        private val context = view.context
        private lateinit var movie: Movie
    }
}