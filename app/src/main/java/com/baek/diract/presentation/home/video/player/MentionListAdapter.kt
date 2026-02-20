package com.baek.diract.presentation.home.video.player

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.baek.diract.databinding.ItemMentionListBinding
import com.baek.diract.domain.model.TeamMemberSummary

class MentionListAdapter(
    private val onMemberClick: (TeamMemberSummary) -> Unit
) : ListAdapter<TeamMemberSummary, MentionListAdapter.MentionViewHolder>(MentionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MentionViewHolder {
        val binding = ItemMentionListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MentionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MentionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MentionViewHolder(
        private val binding: ItemMentionListBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(member: TeamMemberSummary) {
            binding.memberNameTxt.text = if (member.id == FeedbackViewModel.ALL_MEMBER_ID) {
                "@All"
            } else {
                member.name
            }
            binding.root.setOnClickListener {
                onMemberClick(member)
            }
        }
    }

    private class MentionDiffCallback : DiffUtil.ItemCallback<TeamMemberSummary>() {
        override fun areItemsTheSame(
            oldItem: TeamMemberSummary,
            newItem: TeamMemberSummary
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: TeamMemberSummary,
            newItem: TeamMemberSummary
        ): Boolean {
            return oldItem == newItem
        }
    }
}
