package com.tanasi.sflix.adapters.view_holders

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.tanasi.sflix.adapters.SflixAdapter
import com.tanasi.sflix.databinding.ItemCategoryBinding
import com.tanasi.sflix.models.Category

class VhCategory(
    private val _binding: ViewBinding
) : RecyclerView.ViewHolder(
    _binding.root
) {

    private val context = itemView.context
    private lateinit var category: Category

    fun bind(category: Category) {
        this.category = category

        when (_binding) {
            is ItemCategoryBinding -> displayItem(_binding)
        }
    }

    private fun displayItem(binding: ItemCategoryBinding) {
        binding.tvCategoryTitle.text = category.name

        binding.hgvCategory.apply {
            setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
            adapter = SflixAdapter(category.list)
            setItemSpacing(category.itemSpacing)
        }
    }
}