package com.baek.diract.presentation.home.video.player

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.baek.diract.R
import com.baek.diract.databinding.ItemCommentBinding
import com.baek.diract.presentation.common.Formatter.toTimeAgoString
import com.baek.diract.presentation.common.Formatter.toTimeString
import com.google.android.material.chip.Chip

class FeedbackAdapter(
    private val onMoreClick: (FeedbackItem, View) -> Unit = { _, _ -> },
    private val onReplyClick: (FeedbackItem) -> Unit = {},
    private val onTimeChipClick: (FeedbackItem) -> Unit = {},
    private val onRetryClick: (FeedbackItem) -> Unit = {},
    private val onCancelClick: (FeedbackItem) -> Unit = {},
    private val onMoreMentionClick: (FeedbackItem, View) -> Unit = { _, _ -> }
) : ListAdapter<FeedbackItem, FeedbackAdapter.FeedbackViewHolder>(FeedbackDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedbackViewHolder {
        val binding = ItemCommentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FeedbackViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeedbackViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FeedbackViewHolder(
        private val binding: ItemCommentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(feedback: FeedbackItem) {
            when (feedback.status) {
                CommentStatus.SUCCESS -> setSuccessView()
                CommentStatus.FAIL -> setFailView()
                CommentStatus.LOADING -> setLoadingView()
            }
            // 작성자 이름
            binding.nameTxt.text = feedback.author.name

            // 피드백 내용
            binding.commentTxt.text = feedback.content

            // 시간 칩 (구간 또는 포인트)
            bindTimeChip(feedback)

            // 피드백 이미지
            if (feedback.imgUrl != null) {
                binding.feedbackImg.visibility = View.VISIBLE
                val radius = itemView.resources.getDimensionPixelSize(R.dimen.feedback_img_radius)
                Glide.with(itemView)
                    .load(feedback.imgUrl)
                    .transform(com.bumptech.glide.load.resource.bitmap.RoundedCorners(radius))
                    .into(binding.feedbackImg)
            } else {
                binding.feedbackImg.visibility = View.GONE
            }

            // 멘션 칩
            bindMentionChips(feedback)
            // 작성 시간 (상대 시간)
            binding.timeAgoTxt.text = feedback.updatedAt.toTimeAgoString(itemView.context)
            // 댓글 수
            binding.replyBtn.text = feedback.replyCount.toString()

            // 클릭 리스너
            binding.moreBtn.setOnClickListener { view -> onMoreClick(feedback, view) }
            binding.replyBtn.setOnClickListener { onReplyClick(feedback) }
            binding.timeChipBtn.setOnClickListener { onTimeChipClick(feedback) }
            binding.retryBtn.setOnClickListener { onRetryClick(feedback) }
            binding.cancelBtn.setOnClickListener { onCancelClick(feedback) }
            binding.moreMentionChip.setOnClickListener { view ->
                onMoreMentionClick(
                    feedback,
                    view
                )
            }
        }

        private fun setSuccessView() {
            binding.retryBtn.visibility = View.GONE
            binding.cancelBtn.visibility = View.GONE
            binding.timeAgoTxt.visibility = View.VISIBLE
            binding.replyBtn.visibility = View.VISIBLE
            binding.moreBtn.visibility = View.VISIBLE
            binding.loadingTxt.visibility = View.GONE
        }

        private fun setLoadingView() {
            binding.retryBtn.visibility = View.GONE
            binding.cancelBtn.visibility = View.GONE
            binding.timeAgoTxt.visibility = View.GONE
            binding.replyBtn.visibility = View.GONE
            binding.moreBtn.visibility = View.GONE
            binding.loadingTxt.visibility = View.VISIBLE
        }

        private fun setFailView() {
            binding.retryBtn.visibility = View.VISIBLE
            binding.cancelBtn.visibility = View.VISIBLE
            binding.timeAgoTxt.visibility = View.GONE
            binding.replyBtn.visibility = View.GONE
            binding.moreBtn.visibility = View.GONE
            binding.loadingTxt.visibility = View.GONE
        }

        private fun bindTimeChip(feedback: FeedbackItem) {
            val timeText = if (feedback.endTime != null) {
                // 구간 피드백
                "${feedback.startTime.toTimeString()} ~ ${feedback.endTime.toTimeString()}"
            } else {
                // 포인트 피드백
                feedback.startTime.toTimeString()
            }
            binding.timeChipBtn.text = timeText
        }

        private fun bindMentionChips(feedback: FeedbackItem) {
            binding.mentionChipGroup.removeAllViews()

            if (feedback.taggedUsers.isEmpty()) {
                binding.mentionChipGroup.visibility = View.GONE
                binding.moreMentionChip.visibility = View.GONE
                return
            }

            binding.mentionChipGroup.visibility = View.VISIBLE

            val inflater = LayoutInflater.from(binding.root.context)
            feedback.taggedUsers
                .take(MAX_VISIBLE_MENTION)
                .forEach { user ->
                    val chip = inflater.inflate(
                        R.layout.item_mention_chip,
                        binding.mentionChipGroup,
                        false
                    ) as Chip
                    chip.text = "@${user.name}"
                    binding.mentionChipGroup.addView(chip)
                }

            val remain = feedback.taggedUsers.size - MAX_VISIBLE_MENTION
            if (remain > 0) {
                binding.moreMentionChip.visibility = View.VISIBLE
                binding.moreMentionChip.text = "+${remain}"
            }
        }
    }

    private class FeedbackDiffCallback : DiffUtil.ItemCallback<FeedbackItem>() {
        override fun areItemsTheSame(oldItem: FeedbackItem, newItem: FeedbackItem): Boolean {
            return oldItem.feedbackId == newItem.feedbackId
        }

        override fun areContentsTheSame(oldItem: FeedbackItem, newItem: FeedbackItem): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        const val MAX_VISIBLE_MENTION = 2
    }
}
