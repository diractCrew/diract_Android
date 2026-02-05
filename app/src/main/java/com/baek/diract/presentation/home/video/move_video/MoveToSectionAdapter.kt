package com.baek.diract.presentation.home.video.move_video

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.baek.diract.R
import com.baek.diract.databinding.ItemMoveToSectionBinding
import com.baek.diract.domain.model.Section

class MoveToSectionAdapter(
    private val onSectionClick: (Section) -> Unit
) : ListAdapter<Section, MoveToSectionAdapter.SectionViewHolder>(SectionDiffCallback()) {

    private var selectedSectionId: String? = null

    fun setSelectedSection(sectionId: String?) {
        val oldSelectedId = selectedSectionId
        selectedSectionId = sectionId

        // 이전 선택 항목과 새 선택 항목만 갱신
        currentList.forEachIndexed { index, section ->
            if (section.id == oldSelectedId || section.id == sectionId) {
                notifyItemChanged(index, PAYLOAD_SELECTION_CHANGED)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        val binding = ItemMoveToSectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SectionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(
        holder: SectionViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.contains(PAYLOAD_SELECTION_CHANGED)) {
            holder.bindSelectionState(getItem(position))
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    inner class SectionViewHolder(
        private val binding: ItemMoveToSectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(section: Section) {
            binding.sectionNameTxt.text = section.title
            bindSelectionState(section)

            binding.root.setOnClickListener {
                onSectionClick(section)
            }
        }

        fun bindSelectionState(section: Section) {
            val isSelected = section.id == selectedSectionId
            binding.root.isSelected = isSelected
            binding.checkIcon.setImageResource(
                if (isSelected) R.drawable.ic_radio_btn_selected
                else R.drawable.ic_radio_btn_unselected
            )
        }
    }

    class SectionDiffCallback : DiffUtil.ItemCallback<Section>() {
        override fun areItemsTheSame(oldItem: Section, newItem: Section): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Section, newItem: Section): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        private const val PAYLOAD_SELECTION_CHANGED = "payload_selection_changed"
    }
}
