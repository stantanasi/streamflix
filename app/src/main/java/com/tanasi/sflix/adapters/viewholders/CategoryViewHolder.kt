package com.tanasi.sflix.adapters.viewholders

import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.tanasi.sflix.R
import com.tanasi.sflix.adapters.SflixAdapter
import com.tanasi.sflix.databinding.ContentCategorySwiperBinding
import com.tanasi.sflix.databinding.ItemCategoryBinding
import com.tanasi.sflix.fragments.home.HomeFragment
import com.tanasi.sflix.fragments.home.HomeFragmentDirections
import com.tanasi.sflix.models.Category
import com.tanasi.sflix.models.Movie
import com.tanasi.sflix.models.Show
import com.tanasi.sflix.models.TvShow
import com.tanasi.sflix.utils.format
import com.tanasi.sflix.utils.getCurrentFragment
import com.tanasi.sflix.utils.toActivity

class CategoryViewHolder(
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

            is ContentCategorySwiperBinding -> displaySwiper(_binding)
        }
    }


    private fun displayItem(binding: ItemCategoryBinding) {
        binding.tvCategoryTitle.text = category.name

        binding.hgvCategory.apply {
            setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
            adapter = SflixAdapter().apply {
                items.addAll(category.list)
            }
            setItemSpacing(category.itemSpacing)
        }
    }

    private fun displaySwiper(binding: ContentCategorySwiperBinding) {
        val selected = category.list.getOrNull(category.selectedIndex) as? Show ?: return

        binding.tvSwiperTitle.text = when (selected) {
            is Movie -> selected.title
            is TvShow -> selected.title
        }

        binding.tvSwiperRating.text = when (selected) {
            is Movie -> selected.rating?.let { String.format("%.1f", it) } ?: "N/A"
            is TvShow -> selected.rating?.let { String.format("%.1f", it) } ?: "N/A"
        }

        binding.tvSwiperQuality.apply {
            text = when (selected) {
                is Movie -> selected.quality
                is TvShow -> selected.quality
            }
            visibility = when {
                text.isNullOrEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.tvSwiperReleased.apply {
            text = when (selected) {
                is Movie -> selected.released?.format("yyyy")
                is TvShow -> selected.released?.format("yyyy")
            }
            visibility = when {
                text.isNullOrEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.tvSwiperTvShowLastEpisode.apply {
            text = when (selected) {
                is TvShow -> selected.seasons.lastOrNull()?.let { season ->
                    season.episodes.lastOrNull()?.let { episode ->
                        context.getString(
                            R.string.tv_show_item_season_number_episode_number,
                            season.number,
                            episode.number
                        )
                    }
                } ?: context.getString(R.string.tv_show_item_type)
                else -> context.getString(R.string.movie_item_type)
            }
            visibility = when {
                text.isNullOrEmpty() -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.tvSwiperOverview.text = when (selected) {
            is Movie -> selected.overview
            is TvShow -> selected.overview
        }

        binding.btnSwiperWatchNow.apply {
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    when (val fragment = context.toActivity()?.getCurrentFragment()) {
                        is HomeFragment -> fragment.updateBackground(
                            when (selected) {
                                is Movie -> selected.banner
                                is TvShow -> selected.banner
                            }
                        )
                    }
                }
            }
            setOnKeyListener { _, _, event ->
                if (event.action == KeyEvent.ACTION_DOWN) {
                    when (event.keyCode) {
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            category.selectedIndex =
                                (category.selectedIndex + 1) % category.list.size

                            when (val fragment = context.toActivity()?.getCurrentFragment()) {
                                is HomeFragment -> when (val it =
                                    category.list[category.selectedIndex]) {
                                    is Movie -> fragment.updateBackground(it.banner)
                                    is TvShow -> fragment.updateBackground(it.banner)
                                }
                            }
                            bindingAdapter?.notifyItemChanged(bindingAdapterPosition)
                            return@setOnKeyListener true
                        }
                    }
                }
                false
            }
            setOnClickListener {
                findNavController().navigate(
                    when (selected) {
                        is Movie -> HomeFragmentDirections.actionHomeToMovie(selected.id)
                        is TvShow -> HomeFragmentDirections.actionHomeToTvShow(selected.id)
                    }
                )
            }
        }

        binding.llDotsIndicator.apply {
            removeAllViews()
            repeat(category.list.size) { index ->
                val view = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(15, 15).apply {
                        setMargins(10, 0, 10, 0)
                    }
                    setBackgroundResource(R.drawable.bg_dot_indicator)
                    isSelected = (category.selectedIndex == index)
                }
                addView(view)
            }
        }
    }
}