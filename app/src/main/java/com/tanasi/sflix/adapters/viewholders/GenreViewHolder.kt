package com.tanasi.sflix.adapters.viewholders

import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.tanasi.sflix.models.Genre


class GenreViewHolder(
    private val _binding: ViewBinding
) : RecyclerView.ViewHolder(
    _binding.root
) {

    private val context = itemView.context
    private lateinit var genre: Genre

    fun bind(genre: Genre) {
        this.genre = genre

        when (_binding) {
        }
    }
}