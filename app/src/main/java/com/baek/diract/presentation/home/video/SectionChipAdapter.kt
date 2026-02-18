package com.baek.diract.presentation.home.video

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.baek.diract.R
import com.baek.diract.databinding.ItemSectionChipBinding
import com.baek.diract.databinding.ItemSetSectionChipBinding

class SectionChipAdapter(
    private val onSetSectionClick: () -> Unit,
    private val onSectionClick: (SectionChipItem.SectionUi) -> Unit
) : ListAdapter<SectionChipItem, RecyclerView.ViewHolder>(SectionChipDiffCallback) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is SectionChipItem.SetSection -> VIEW_TYPE_ADD
            is SectionChipItem.SectionUi -> VIEW_TYPE_SECTION
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_ADD -> AddSectionViewHolder(
                ItemSetSectionChipBinding.inflate(inflater, parent, false)
            )

            VIEW_TYPE_SECTION -> SectionUiViewHolder(
                ItemSectionChipBinding.inflate(inflater, parent, false)
            )

            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is SectionChipItem.SetSection -> (holder as AddSectionViewHolder).bind()
            is SectionChipItem.SectionUi -> (holder as SectionUiViewHolder).bind(item)
        }
    }

    inner class AddSectionViewHolder(
        private val binding: ItemSetSectionChipBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind() {
            binding.root.setOnClickListener {
                onSetSectionClick()
            }
        }
    }

    inner class SectionUiViewHolder(
        private val binding: ItemSectionChipBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SectionChipItem.SectionUi) {
            binding.root.text = if (item.isDefault) {
                binding.root.context.getString(R.string.section_default_name)
            } else {
                item.name
            }
            binding.root.isSelected = item.isSelected
            binding.root.setOnClickListener {
                onSectionClick(item)
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_ADD = 0
        private const val VIEW_TYPE_SECTION = 1

        object SectionChipDiffCallback : DiffUtil.ItemCallback<SectionChipItem>() {
            override fun areItemsTheSame(
                oldItem: SectionChipItem,
                newItem: SectionChipItem
            ): Boolean {
                return when {
                    oldItem is SectionChipItem.SetSection && newItem is SectionChipItem.SetSection -> true
                    oldItem is SectionChipItem.SectionUi && newItem is SectionChipItem.SectionUi -> oldItem.id == newItem.id
                    else -> false
                }
            }

            override fun areContentsTheSame(
                oldItem: SectionChipItem,
                newItem: SectionChipItem
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}
