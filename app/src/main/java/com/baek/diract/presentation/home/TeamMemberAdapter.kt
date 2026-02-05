package com.baek.diract.presentation.home

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.baek.diract.R
import com.baek.diract.databinding.ItemTeammemberBinding

data class TeamMemberUi(
    val id: String,
    val name: String,
    val isLeader: Boolean
)

class TeamMemberAdapter(
    private val onMoreClick: (anchor: View, member: TeamMemberUi) -> Unit,
    private val onSelectionChanged: (selectedIds: Set<String>) -> Unit
) : RecyclerView.Adapter<TeamMemberAdapter.VH>() {

    private val items = mutableListOf<TeamMemberUi>()
    private var isKickMode: Boolean = false
    private val selectedIds = linkedSetOf<String>()

    // ✅ "현재 사용자"가 팀장인지 여부 (Fragment에서 setLeaderUser로 주입)
    private var isLeaderUser: Boolean = false

    fun setLeaderUser(isLeader: Boolean) {
        isLeaderUser = isLeader
        notifyDataSetChanged()
    }

    fun submitList(list: List<TeamMemberUi>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun setKickMode(enabled: Boolean) {
        isKickMode = enabled
        if (!enabled) selectedIds.clear()
        notifyDataSetChanged()
        onSelectionChanged(selectedIds.toSet())
    }

    fun getSelectedIds(): Set<String> = selectedIds.toSet()

    private fun toggleSelection(memberId: String) {
        if (!isKickMode) return
        if (selectedIds.contains(memberId)) selectedIds.remove(memberId) else selectedIds.add(memberId)
        notifyDataSetChanged()
        onSelectionChanged(selectedIds.toSet())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemTeammemberBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding, onMoreClick, ::toggleSelection)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(
            item = items[position],
            isKickMode = isKickMode,
            selectedIds = selectedIds,
            isLeaderUser = isLeaderUser
        )
    }

    override fun getItemCount() = items.size

    class VH(
        private val binding: ItemTeammemberBinding,
        private val onMoreClick: (View, TeamMemberUi) -> Unit,
        private val onToggleSelection: (memberId: String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: TeamMemberUi,
            isKickMode: Boolean,
            selectedIds: Set<String>,
            isLeaderUser: Boolean
        ) = with(binding) {

            tvName.text = item.name
            ivLeader.isVisible = item.isLeader

            if (isKickMode) {
                // 킥 모드에서는 ... 버튼 숨김(선택 UI만)
                ivMore.isVisible = false

                if (item.isLeader) {
                    // 팀장은 선택 불가(아이콘 자리만 유지)
                    checkMember.isInvisible = true
                    root.setOnClickListener(null)
                    checkMember.setOnClickListener(null)
                } else {
                    checkMember.isInvisible = false
                    val selected = selectedIds.contains(item.id)

                    val colorRes = if (selected) R.color.secondary_normal else R.color.fill_assistive
                    ImageViewCompat.setImageTintList(
                        checkMember,
                        ColorStateList.valueOf(ContextCompat.getColor(root.context, colorRes))
                    )

                    root.setOnClickListener { onToggleSelection(item.id) }
                    checkMember.setOnClickListener { onToggleSelection(item.id) }
                }
            } else {
                // 기본 모드: 팀장/팀원 모두 ... 버튼은 보이되,
                // "현재 사용자가 팀장"일 때만 활성화(팀원이면 회색+클릭 막힘)
                checkMember.isVisible = false
                ivMore.isVisible = !item.isLeader

                ivMore.isEnabled = isLeaderUser
                ivMore.alpha = if (isLeaderUser) 1f else 0f

                root.setOnClickListener(null)
                checkMember.setOnClickListener(null)

                ivMore.setOnClickListener { v ->
                    if (!isLeaderUser) return@setOnClickListener
                    onMoreClick(v, item)
                }
            }
        }
    }
}
