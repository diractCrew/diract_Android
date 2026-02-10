package com.baek.diract.presentation.home.video.player

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.baek.diract.R
import com.baek.diract.databinding.ItemReplyBinding
import com.baek.diract.presentation.common.Formatter.toTimeAgoString

class ReplyAdapter(
    private val onMoreClick: (ReplyItem, View) -> Unit = { _, _ -> },
    private val onReplyClick: (ReplyItem) -> Unit = {},
    private val onRetryClick: (ReplyItem) -> Unit = {},
    private val onCancelClick: (ReplyItem) -> Unit = {}
) : ListAdapter<ReplyItem, ReplyAdapter.ReplyViewHolder>(ReplyDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReplyViewHolder {
        val binding = ItemReplyBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReplyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReplyViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ReplyViewHolder(
        private val binding: ItemReplyBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(reply: ReplyItem) {
            when (reply.status) {
                CommentStatus.SUCCESS -> setSuccessView()
                CommentStatus.FAIL -> setFailView()
                CommentStatus.LOADING -> setLoadingView()
            }

            binding.nameTxt.text = reply.author.name
            binding.replyTxt.text = buildReplyText(reply)
            binding.timeAgoTxt.text = reply.createdAt.toTimeAgoString(itemView.context)

            binding.moreBtn.setOnClickListener { view -> onMoreClick(reply, view) }
            binding.replyBtn.setOnClickListener { onReplyClick(reply) }
            binding.retryBtn.setOnClickListener { onRetryClick(reply) }
            binding.cancelBtn.setOnClickListener { onCancelClick(reply) }
        }

        private fun buildReplyText(reply: ReplyItem): CharSequence {
            if (reply.taggedUsers.isEmpty()) return reply.content

            val blueColor = ContextCompat.getColor(itemView.context, R.color.accent_blue_strong)
            val spannable = SpannableStringBuilder()

            reply.taggedUsers.forEach { user ->
                val mention = "@${user.name} "
                val start = spannable.length
                spannable.append(mention)
                spannable.setSpan(
                    ForegroundColorSpan(blueColor),
                    start,
                    spannable.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            spannable.append(reply.content)
            return spannable
        }

        private fun setSuccessView() {
            binding.retryBtn.visibility = View.GONE
            binding.cancelBtn.visibility = View.GONE
            binding.timeAgoTxt.visibility = View.VISIBLE
            binding.replyBtn.visibility = View.VISIBLE
            binding.moreBtn.visibility = View.VISIBLE
        }

        private fun setLoadingView() {
            binding.retryBtn.visibility = View.GONE
            binding.cancelBtn.visibility = View.GONE
            binding.timeAgoTxt.visibility = View.GONE
            binding.replyBtn.visibility = View.GONE
            binding.moreBtn.visibility = View.GONE
        }

        private fun setFailView() {
            binding.retryBtn.visibility = View.VISIBLE
            binding.cancelBtn.visibility = View.VISIBLE
            binding.timeAgoTxt.visibility = View.GONE
            binding.replyBtn.visibility = View.GONE
            binding.moreBtn.visibility = View.GONE
        }
    }

    private class ReplyDiffCallback : DiffUtil.ItemCallback<ReplyItem>() {
        override fun areItemsTheSame(oldItem: ReplyItem, newItem: ReplyItem): Boolean {
            return oldItem.replyId == newItem.replyId
        }

        override fun areContentsTheSame(oldItem: ReplyItem, newItem: ReplyItem): Boolean {
            return oldItem == newItem
        }
    }
}
