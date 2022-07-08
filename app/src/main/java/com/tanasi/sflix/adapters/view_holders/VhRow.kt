package com.tanasi.sflix.adapters.view_holders

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.tanasi.sflix.adapters.SflixAdapter
import com.tanasi.sflix.databinding.ItemRowBinding
import com.tanasi.sflix.models.Row

class VhRow(
    private val _binding: ViewBinding
) : RecyclerView.ViewHolder(
    _binding.root
) {

    private val context = itemView.context
    private lateinit var row: Row

    fun bind(row: Row) {
        this.row = row

        when (_binding) {
            is ItemRowBinding -> displayRow(_binding)
        }
    }

    private fun displayRow(binding: ItemRowBinding) {
        binding.tvRowTitle.text = row.title

        binding.hgvRow.apply {
            setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
            adapter = SflixAdapter(row.list)
            setItemSpacing(row.itemSpacing)
        }
    }
}