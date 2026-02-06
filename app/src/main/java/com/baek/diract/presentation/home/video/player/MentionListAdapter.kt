package com.baek.diract.presentation.home.video.player

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.baek.diract.databinding.ItemMentionListBinding
import com.baek.diract.domain.model.FeedbackUser

class MentionListAdapter(
    private val onMemberClick: (FeedbackUser) -> Unit
) : ListAdapter<FeedbackUser, MentionListAdapter.MentionViewHolder>(MentionDiffCallback()) {

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

        fun bind(member: FeedbackUser) {
            binding.memberNameTxt.text = if (member.userId == FeedbackViewModel.ALL_MEMBER_ID) {
                "@All"
            } else {
                member.name ?: ""
            }
            binding.root.setOnClickListener {
                onMemberClick(member)
            }
        }
    }

    private class MentionDiffCallback : DiffUtil.ItemCallback<FeedbackUser>() {
        override fun areItemsTheSame(oldItem: FeedbackUser, newItem: FeedbackUser): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: FeedbackUser, newItem: FeedbackUser): Boolean {
            return oldItem == newItem
        }
    }
}
