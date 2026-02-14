package com.baek.diract.presentation.home

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.baek.diract.R
import com.baek.diract.databinding.FragmentManageTeamspaceBinding
import com.baek.diract.presentation.common.CustomToast
import com.baek.diract.presentation.common.LoadingOverlay
import com.baek.diract.presentation.common.ToastEvent
import com.baek.diract.presentation.common.UiState
import com.baek.diract.presentation.common.dialog.BasicDialog
import com.baek.diract.presentation.common.dialog.InputDialogFragment
import com.baek.diract.presentation.common.option.OptionItem
import com.baek.diract.presentation.common.option.OptionPopup
import com.baek.diract.presentation.common.option.TeamspaceSwitcherPopup
import com.baek.diract.presentation.common.option.TeamspaceUi
import com.google.android.material.divider.MaterialDividerItemDecoration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ManageTeamspaceFragment : Fragment(R.layout.fragment_manage_teamspace) {

    private var selectedTeamspaceId: String = ""   // ✅ UUID String
    private var selectedTeamspaceName: String = "" // ✅ 표시용 이름
    private var createTeamspaceDialog: InputDialogFragment? = null
    private var renameTeamspaceDialog: InputDialogFragment? = null
    private var pendingRenameName: String? = null
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

        // 상단 메뉴(더보기)는 팀장만
        binding.toolbar.menu.findItem(R.id.action_more)?.isVisible = isLeader
        binding.toolbar.menu.findItem(R.id.action_more)?.isEnabled = isLeader

        // ✅ 팀장은 나가기 버튼 숨김(원하면)
        if (!isKickMode) {
            binding.dividerDangerActions.visibility = View.VISIBLE
            binding.tvLeaveTeamspace.visibility = View.VISIBLE   // 팀장이어도 '보이기'는 유지 (누르면 다이얼로그)
            binding.tvDeleteTeamspace.isVisible = isLeader       // 삭제는 팀장만
        }
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
        viewModel.resetCreateTeamspaceUiState()

        createTeamspaceDialog = InputDialogFragment.newInstance(
            title = getString(R.string.teamspace_create_title),
            description = getString(R.string.teamspace_create_prompt),
            hint = getString(R.string.teamspace_name_hint),
            buttonText = getString(R.string.teamspace_create_cta),
            maxLength = 20
        ).apply {
            onConfirm = { name ->
                viewModel.createTeamspace(name)
            }
        }
        createTeamspaceDialog?.show(parentFragmentManager, InputDialogFragment.TAG)
    }

    private fun renderMembers(list: List<TeamMemberUi>) {
        val sorted = list.sortedByDescending { it.isLeader }
        memberAdapter.submitList(sorted)

        val showEmpty = (sorted.size == 1 && sorted.first().isLeader)
        binding.emptyMember.isVisible = showEmpty


    }


    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun showRenameTeamspaceDialog() {
        viewModel.resetRenameTeamspaceUiState()

        val currentName = binding.tvTeamspaceTitle.text?.toString().orEmpty()

        renameTeamspaceDialog = InputDialogFragment.newInstance(
            title = getString(R.string.teamspace_rename_title),
            description = getString(R.string.teamspace_create_prompt),
            hint = null,
            buttonText = getString(R.string.confirm),
            maxLength = 20,
            initialText = currentName
        ).apply {
            onConfirm = { newName ->
                pendingRenameName = newName
                viewModel.renameTeamspace(newName)

            }
        }

        renameTeamspaceDialog?.show(parentFragmentManager, InputDialogFragment.TAG)
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // 1) 로딩/스와이프 종료
                launch {
                    viewModel.uiState.collect { state ->
                        loadingOverlay.setVisible(state is UiState.Loading)
                        if (state !is UiState.Loading) {
                            binding.swipeRefresh.isRefreshing = false
                        }
                    }
                }
                launch {
                    viewModel.isLeader.collect { isLeader ->
                        applyRoleUi(isLeader)
                        memberAdapter.setLeaderUser(isLeader)
                        if (!isLeader) exitKickMode()
                        else if (!isKickMode) exitKickMode() // ✅ 팀장도 초기 UI를 exitKickMode 기준으로 한번 정렬
                    }
                }

                // 2) 토스트(1회성)
                launch {
                    viewModel.toastMessage.collect { event ->
                        if (event.isErr) CustomToast.showNegative(requireContext(), event.txtRes)
                        else CustomToast.showPositive(requireContext(), event.txtRes)
                    }
                }

                // 3) 네비(1회성) - VM에 navEvent가 있을 때만
                launch {
                    viewModel.navEvent.collect { event ->
                        when (event) {
                            NavEvent.Close -> findNavController().navigateUp()
                        }
                    }
                }

                // 4) 멤버 목록 렌더
                launch {
                    viewModel.members.collect { list ->
                        renderMembers(list)
                    }
                }
                launch {
                    viewModel.createTeamspaceUiState.collect { state ->
                        when (state) {
                            is UiState.None -> createTeamspaceDialog?.showDefault()
                            is UiState.Loading -> createTeamspaceDialog?.showLoading()
                            is UiState.Success -> {
                                createTeamspaceDialog?.showComplete()
                                delay(800)
                                createTeamspaceDialog?.dismiss()
                                createTeamspaceDialog = null
                                viewModel.resetCreateTeamspaceUiState()
                            }
                            is UiState.Error -> {
                                createTeamspaceDialog?.showDefault()
                                viewModel.resetCreateTeamspaceUiState()
                            }
                        }
                    }
                }

                launch {
                    viewModel.renameTeamspaceUiState.collect { state ->
                        when (state) {
                            is UiState.None -> renameTeamspaceDialog?.showDefault()
                            is UiState.Loading -> renameTeamspaceDialog?.showLoading()
                            is UiState.Success -> {
                                pendingRenameName?.let { binding.tvTeamspaceTitle.text = it }
                                pendingRenameName = null

                                renameTeamspaceDialog?.showComplete()
                                delay(800)
                                renameTeamspaceDialog?.dismiss()
                                renameTeamspaceDialog = null
                                viewModel.resetRenameTeamspaceUiState()
                            }
                            is UiState.Error -> {
                                renameTeamspaceDialog?.showDefault()
                                viewModel.resetRenameTeamspaceUiState()
                            }
                        }
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

        // ✅ Home에서 넘어온 teamspaceId/name 받기 (도메인 모델: id, name)
        selectedTeamspaceId = arguments?.getString("teamspaceId").orEmpty()
        selectedTeamspaceName = arguments?.getString("teamspaceName").orEmpty()

        // ✅ 제목에 반영 (연동)
        if (selectedTeamspaceName.isNotBlank()) {
            binding.tvTeamspaceTitle.text = selectedTeamspaceName
        }

        // RecyclerView
        binding.rvMembers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = memberAdapter
        }

        // 상태 수집
        observeState()

        // ✅ 팀스페이스 id 세팅 후 멤버 로드
        if (selectedTeamspaceId.isNotBlank()) {
            viewModel.setTeamspaceId(selectedTeamspaceId)
            viewModel.loadMembers()
        }
        viewModel.loadTeamspaces()

        // divider
        if (!dividerAdded) {
            val divider = MaterialDividerItemDecoration(requireContext(), RecyclerView.VERTICAL).apply {
                setDividerColor(ContextCompat.getColor(requireContext(), R.color.stroke_strong))
                setDividerThickness(dp(1))
                setDividerInsetStart(dp(24))
                setDividerInsetEnd(dp(24))
                isLastItemDecorated = false
            }
            binding.rvMembers.addItemDecoration(divider)
            dividerAdded = true
        }

        // toolbar menu
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_more -> {
                    val anchor = binding.toolbar.findViewById<View>(R.id.action_more) ?: binding.toolbar
                    OptionPopup.builder(requireContext())
                        .addOptions(
                            OptionItem.renameTeamspace(),
                            OptionItem.kickMember()
                        )
                        .setOnOptionSelectedListener { option ->
                            when (option.id) {
                                OptionItem.ID_RENAME_TEAMSPACE -> showRenameTeamspaceDialog()
                                OptionItem.ID_KICK_MEMBER -> enterKickMode()
                            }
                        }
                        .show(anchor)
                    true
                }
                else -> false
            }
        }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // swipe refresh
        binding.swipeRefresh.setOnRefreshListener {
            if (selectedTeamspaceId.isNotBlank()) viewModel.loadMembers()
            else binding.swipeRefresh.isRefreshing = false
        }


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
                    viewModel.setTeamspaceId(selectedTeamspaceId)
                    viewModel.leaveTeamspace()
                }
            ).show()
        }
        binding.tvDeleteTeamspace.setOnClickListener {

            if (!isLeaderUser) return@setOnClickListener

            val teamName = binding.tvTeamspaceTitle.text?.toString().orEmpty()

            showDeleteTeamspaceDialog(teamName) {
                viewModel.deleteTeamspace()
            }
        }

        binding.teamspaceTitleArea.setOnClickListener {
            val items = viewModel.teamspaces.value.map { ts ->
                TeamspaceUi(id = ts.id, name = ts.name)
            }

            TeamspaceSwitcherPopup(
                context = requireContext(),
                items = items,
                selectedId = selectedTeamspaceId,
                onSelect = { selected ->
                    selectedTeamspaceId = selected.id
                    selectedTeamspaceName = selected.name
                    binding.tvTeamspaceTitle.text = selected.name
                    viewModel.saveLastTeamspaceId(selectedTeamspaceId)
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
