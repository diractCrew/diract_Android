package com.baek.diract.presentation.home

import android.R.attr.visibility
import android.R.id.message
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.baek.diract.R
import com.baek.diract.databinding.FragmentManageTeamspaceBinding
import com.baek.diract.presentation.common.CustomToast
import com.baek.diract.presentation.common.LoadingOverlay
import com.baek.diract.presentation.common.UiState
import com.baek.diract.presentation.common.dialog.BasicDialog

import com.baek.diract.presentation.common.dialog.InputDialogFragment
import com.baek.diract.presentation.common.option.OptionItem
import com.baek.diract.presentation.common.option.OptionPopup
import com.baek.diract.presentation.common.option.TeamspaceSwitcherPopup
import com.baek.diract.presentation.common.option.TeamspaceUi
import com.baek.diract.presentation.common.recyclerview.SpacingItemDecoration
import com.google.android.material.divider.MaterialDividerItemDecoration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ManageTeamspaceFragment : Fragment(R.layout.fragment_manage_teamspace) {

//    private var selectedTeamspaceId: Long = 3L // 임시(처음 선택값)
    private val teamspaces = listOf(
        TeamspaceUi(1, "Diract Crew"),
        TeamspaceUi(2, "Developer Academy"),
        TeamspaceUi(3, "CTR D"),
    )
    private var isLeaderUser: Boolean = true // TODO: 실제 서버/도메인값으로 세팅
    private var _binding: FragmentManageTeamspaceBinding? = null
    private val viewModel: ManageTeamspaceViewModel by viewModels()
    private val loadingOverlay by lazy { LoadingOverlay(this) }
    private val binding get() = _binding!!
    private var switcherPopup: TeamspaceSwitcherPopup? = null
    private var isKickMode: Boolean = false
    private fun updateKickActionEnabled(enabled: Boolean) {
        binding.actionKickMembers.isEnabled = enabled
        val colorRes = if (enabled) R.color.accent_red_normal else R.color.fill_assistive
        binding.actionKickMembers.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
    }
    private fun applyRoleUi(isLeader: Boolean) {
        isLeaderUser = isLeader


        // ✅ "팀 스페이스 삭제하기" 제거
        binding.toolbar.menu.findItem(R.id.action_more)?.isVisible = isLeader
        binding.toolbar.menu.findItem(R.id.action_more)?.isEnabled = isLeader

        binding.tvDeleteTeamspace.isVisible = isLeader
    }
    private fun exitKickMode() {
        isKickMode = false
        binding.cancelBtn.isVisible = false
        binding.actionKickMembers.isVisible = false

        binding.dividerDangerActions.visibility = View.VISIBLE
        binding.tvLeaveTeamspace.visibility = View.VISIBLE

        // ✅ 리더만 삭제 보이게
        binding.tvDeleteTeamspace.isVisible = isLeaderUser

        updateKickActionEnabled(false)
        memberAdapter.setKickMode(false)
    }
    private val memberAdapter by lazy {
        TeamMemberAdapter(
            onMoreClick = { _, member ->
                if (!isLeaderUser) return@TeamMemberAdapter
                MemberActionBottomSheet
                    .newInstance(member.id, member.name) { id, name ->
                        showKickMemberDialog(name) {
                            // TODO: viewModel.kickMember(teamspaceId, id)
                        }
                    }
                    .show(parentFragmentManager, "member_actions")
            },
            onSelectionChanged = { selectedIds ->
                updateKickActionEnabled(selectedIds.isNotEmpty())
            }
        )
    }
    private fun enterKickMode() {
        isKickMode = true
        binding.cancelBtn.isVisible = true
        binding.actionKickMembers.isVisible = true
        binding.dividerDangerActions.visibility = View.GONE
        binding.tvLeaveTeamspace.visibility = View.GONE
        binding.tvDeleteTeamspace.visibility = View.GONE
        updateKickActionEnabled(false)
        memberAdapter.setKickMode(true)
    }
    private fun showCreateTeamspaceSheet() {
        InputDialogFragment.newInstance(
            title = getString(R.string.teamspace_create_title),
            description = getString(R.string.teamspace_create_prompt),
            hint = getString(R.string.teamspace_name_hint),
            buttonText = getString(R.string.teamspace_create_cta),
            maxLength = 20
        ).apply {
            onConfirm = { name ->
                viewModel.createTeamspace(name)
            }
        }.show(parentFragmentManager, InputDialogFragment.TAG)
    }
    private fun renderMembers(list: List<TeamMemberUi>) {
        val sorted = list.sortedByDescending { it.isLeader }
        memberAdapter.submitList(sorted)

        val showEmpty = (sorted.size == 1 && sorted.first().isLeader)

        binding.emptyMember.isVisible = showEmpty
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun showRenameTeamspaceDialog() {
        val currentName = binding.tvTeamspaceTitle.text?.toString().orEmpty()

        val dialog = InputDialogFragment.newInstance(
            title = getString(R.string.teamspace_rename_title),
            description = getString(R.string.teamspace_create_prompt),
            hint = null, // 현재 팀스페이스 이름이 들어가야함
            buttonText = getString(R.string.confirm),
            maxLength = 20,
            initialText = currentName
        ).apply {
            onConfirm = { newName ->
                // ✅ 일단 화면만 동작시키기(추후 ViewModel로 rename API 연결)
                binding.tvTeamspaceTitle.text = newName
                dismiss()
            }
        }

        dialog.show(parentFragmentManager, InputDialogFragment.TAG)
    }
    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // 1) 로딩 오버레이
                launch {
                    viewModel.state.collect { state ->
                        loadingOverlay.setVisible(state is UiState.Loading)

                        // 당겨서 새로고침 쓰면 여기서 종료 처리
                        if (state !is UiState.Loading) {
                            binding.swipeRefresh.isRefreshing = false
                        }

                        // (선택) 성공/에러 후 추가 처리 필요하면 when으로
                        when (state) {
                            is UiState.Error -> {
                                // 에러 UI 필요하면 여기서 처리 가능
                                // state.message 사용 가능
                            }
                            else -> Unit
                        }
                    }
                }

                // 2) 토스트(1회성)
                launch {
                    viewModel.toast.collect { msgRes ->
                        CustomToast.showNegative(requireContext(), msgRes)
                    }
                }

                // 3) 네비(1회성)
                launch {
                    viewModel.nav.collect { event ->
                        when (event) {
                            NavEvent.Close -> findNavController().navigateUp()
                        }
                    }
                }

                // 4) 멤버 목록 (VM에서 내려주면 여기 연결)
                launch {
                    viewModel.members.collect { list ->
                        renderMembers(list) // <- 너가 이미 만든 함수
                    }
                }
            }
        }
    }
    private var dividerAdded = false
    private fun showKickMemberDialog(memberName: String, onConfirm: () -> Unit) {
        BasicDialog.destructive(
            context = requireContext(),
            title = getString(R.string.dialog_teamspace_kick_title, memberName),
            message = getString(R.string.dialog_teamspace_kick_message),
            negativeText = getString(R.string.cancel),
            positiveText = getString(R.string.dialog_teamspace_kick_action), // "내보내기" 리소스
            onNegative = {},
            onPositive = onConfirm
        ).show()
    }

    private fun showDeleteTeamspaceDialog(teamName: String, onConfirm: () -> Unit) {
        BasicDialog.destructive(
            context = requireContext(),
            title = getString(R.string.dialog_teamspace_delete_title, teamName),
            message = getString(R.string.dialog_teamspace_delete_message),
            negativeText = getString(R.string.cancel),
            positiveText = getString(R.string.dialog_delete), // "삭제" 리소스(이미 있던걸로)
            onNegative = {},
            onPositive = onConfirm
        ).show()
    }

    private fun showLeaderCannotLeaveDialog() {
        BasicDialog.confirm(
            context = requireContext(),
            title = getString(R.string.dialog_teamspace_leader_cannot_leave_title),
            message = getString(R.string.dialog_teamspace_leader_cannot_leave_message)
        ).show()
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentManageTeamspaceBinding.bind(view)
        binding.rvMembers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = memberAdapter
        }
        observeState()
        viewModel.setTeamspaceId(selectedTeamspaceId)

        if (!dividerAdded) {
            val divider = MaterialDividerItemDecoration(requireContext(), RecyclerView.VERTICAL).apply {
                setDividerColor(ContextCompat.getColor(requireContext(), R.color.stroke_strong))
                setDividerThickness(dp(1))

                // ✅ 피그마처럼 좌우 여백 주기 (값은 너 리스트 좌우 padding과 맞춰)
                setDividerInsetStart(dp(24))
                setDividerInsetEnd(dp(24))

                isLastItemDecorated = false
            }
            binding.rvMembers.addItemDecoration(divider)
            dividerAdded = true
        }


        // ✅ 상단바 메뉴 클릭 처리 (여기에)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_more -> {
                    val anchor = binding.toolbar.findViewById<View>(R.id.action_more) ?: binding.toolbar

                    OptionPopup.builder(requireContext())
                        .addOptions(
                            OptionItem.renameTeamspace(),  // "팀 스페이스 이름 수정"
                            OptionItem.kickMember()        // "팀원 내보내기"(빨강)
                        )
                        .setOnOptionSelectedListener { option ->
                            when (option.id) {
                                OptionItem.ID_RENAME_TEAMSPACE -> {
                                    showRenameTeamspaceDialog()
                                }
                                OptionItem.ID_KICK_MEMBER -> {    enterKickMode()
                                }
                            }
                        }
                        .show(anchor)

                    true
                }
                else -> false
            }
        }
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()   // 또는 popBackStack()
        }
        // 1) RecyclerView 세팅


        // 2) 당겨서 새로고침 테스트(동작만)
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadMembers()
        }

        // 3) 더미 10개 넣기
        val fake = listOf(
            TeamMemberUi("1", "카단", isLeader = true),
            TeamMemberUi("2", "줄리엔", isLeader = false),
            TeamMemberUi("3", "리비", isLeader = false),
            TeamMemberUi("4", "제이콥", isLeader = false),
            TeamMemberUi("5", "파이디온", isLeader = false),
            TeamMemberUi("6", "벨코", isLeader = false),
            TeamMemberUi("7", "멤버7", isLeader = false),
            TeamMemberUi("8", "멤버8", isLeader = false),
            TeamMemberUi("9", "멤버9", isLeader = false),
            TeamMemberUi("10", "멤버10", isLeader = false),
        )
        val myMemberId = "2" // TODO: 실제 내 id
        val isLeader = fake.firstOrNull { it.id == myMemberId }?.isLeader == true
        applyRoleUi(isLeader)
        memberAdapter.setLeaderUser(isLeader)
        binding.tvLeaveTeamspace.setOnClickListener {

            // ✅ 팀장인 경우: '나갈 수 없음'만 띄우고 끝
            if (isLeaderUser) {
                showLeaderCannotLeaveDialog()
                return@setOnClickListener
            }

            val teamName = binding.tvTeamspaceTitle.text?.toString().orEmpty()

            val title = if (teamName.isNotBlank()) {
                "$teamName ${getString(R.string.dialog_teamspace_leave_teamspace_title)}"
            } else {
                getString(R.string.dialog_teamspace_leave_teamspace_title)
            }

            BasicDialog.destructive(
                context = requireContext(),
                title = title,
                message = getString(R.string.dialog_teamspace_leave_teamspace_message),
                negativeText = getString(R.string.dialog_cancel),
                positiveText = getString(R.string.dialog_teamspace_leave_teamspace),
                onNegative = {},
                onPositive = {
                    viewModel.leaveTeamspace()
                }
            ).show()
        }
        binding.tvDeleteTeamspace.setOnClickListener {

            // 리더만 삭제 가능 (팀원이면 원래 뷰도 숨김 처리 중이지만 안전하게 한 번 더)
            if (!isLeaderUser) return@setOnClickListener

            val teamName = binding.tvTeamspaceTitle.text?.toString().orEmpty()

            showDeleteTeamspaceDialog(teamName) {
                viewModel.deleteTeamspace()
            }
        }
// 팀장 맨 위 고정해서 넣기
        memberAdapter.submitList(fake.sortedByDescending { it.isLeader })
        binding.teamspaceTitleArea.setOnClickListener {
            TeamspaceSwitcherPopup(
                context = requireContext(),
                items = teamspaces,
                selectedId = selectedTeamspaceId,
                onSelect = { selected ->
                    selectedTeamspaceId = selected.id
                    binding.tvTeamspaceTitle.text = selected.name
                    viewModel.setTeamspaceId(selectedTeamspaceId)
                    viewModel.loadMembers()
                },
                onCreate = { showCreateTeamspaceSheet() }
            ).show(binding.teamspaceTitleArea)
        }
        exitKickMode()
        binding.cancelBtn.setOnClickListener { exitKickMode() }

        binding.actionKickMembers.setOnClickListener {
            if (!binding.actionKickMembers.isEnabled) return@setOnClickListener

            val selectedIds = memberAdapter.getSelectedIds()

            BasicDialog.destructive(
                context = requireContext(),
                title = getString(R.string.teamspace_kick_members_title),
                message = getString(R.string.teamspace_kick_members_desc),
                negativeText = getString(R.string.cancel),
                positiveText = getString(R.string.teamspace_action_kick_btn),
                onNegative = { /* 아무 것도 안 해도 됨 */ },
                onPositive = {
                    viewModel.kickMembers(selectedIds.toList())
                    exitKickMode()
                }
            ).show()
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
