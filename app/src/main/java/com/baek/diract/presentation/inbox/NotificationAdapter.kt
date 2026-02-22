package com.baek.diract.presentation.inbox

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.baek.diract.R
import com.baek.diract.databinding.ItemNotificationBinding
import com.baek.diract.domain.model.Notification
import com.baek.diract.domain.model.NotificationType
import com.baek.diract.presentation.common.Formatter.toTimeAgoString

class NotificationAdapter(
    private val onClick: (Notification) -> Unit
) : ListAdapter<Notification, NotificationAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: Notification) {
            binding.replyIcon.visibility =
                if (notification.type == NotificationType.FEEDBACK) View.GONE else View.VISIBLE
            binding.feedbackIcon.visibility =
                if (notification.type == NotificationType.FEEDBACK) View.VISIBLE else View.GONE

            binding.teamspaceNameTxt.text = notification.teamspaceName
            val titleRes = when (notification.type) {
                NotificationType.FEEDBACK -> if (notification.videoTitle == null) R.string.inbox_feedback_title else R.string.inbox_feedback_title_with_video
                NotificationType.REPLY -> if (notification.videoTitle == null) R.string.inbox_reply_title else R.string.inbox_reply_title_with_video
            }
            binding.titleTxt.text = if (notification.videoTitle != null) {
                binding.root.context.getString(
                    titleRes,
                    notification.videoTitle,
                    notification.senderName
                )
            } else {
                binding.root.context.getString(titleRes, notification.senderName)
            }
            binding.timeAgoTxt.text = notification.createdAt.toTimeAgoString(binding.root.context)

            binding.root.setBackgroundColor(
                if (!notification.isRead) {
                    binding.root.context.getColor(R.color.fill_alternative)
                } else {
                    binding.root.context.getColor(R.color.background_normal)
                }
            )
            binding.root.setOnClickListener { onClick(notification) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Notification>() {
        override fun areItemsTheSame(oldItem: Notification, newItem: Notification) =
            oldItem.notificationId == newItem.notificationId

        override fun areContentsTheSame(oldItem: Notification, newItem: Notification) =
            oldItem == newItem
    }
}
