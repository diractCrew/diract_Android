package com.baek.diract.presentation.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
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
import com.google.android.material.divider.MaterialDividerItemDecoration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ManageTeamspaceFragment : Fragment(R.layout.fragment_manage_teamspace) {

    private var _binding: FragmentManageTeamspaceBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ManageTeamspaceViewModel by viewModels()
    private val loadingOverlay by lazy { LoadingOverlay(this) }

    private var memberActionSheet: MemberActionBottomSheet? = null
    private var createTeamspaceDialog: InputDialogFragment? = null
    private var renameTeamspaceDialog: InputDialogFragment? = null
    private var pendingRenameName: String? = null

    private var selectedTeamspaceId: String = ""
    private var selectedTeamspaceName: String = ""

    private var isLeaderUser: Boolean = false
    private var isKickMode: Boolean = false
    private var dividerAdded = false

    private val memberAdapter by lazy {
        TeamMemberAdapter(
            onMoreClick = { _, member ->
                if (!isLeaderUser) return@TeamMemberAdapter

                val sheet = MemberActionBottomSheet.newInstance(
                    teamspaceId = selectedTeamspaceId,
                    memberId = member.id,
                    memberName = member.name,
                    onGiveLeaderClick = { teamspaceId, newOwnerId ->
                        viewModel.setTeamspaceId(teamspaceId)
                        viewModel.transferLeader(newOwnerId)
                    },
                    onKickClick = { id, name ->
                        showKickMemberDialog(name) { viewModel.kickMembers(listOf(id)) }
                    }
                )

                memberActionSheet = sheet
                sheet.show(parentFragmentManager, "member_actions")
            },
            onSelectionChanged = { selectedIds ->
                updateKickActionEnabled(selectedIds.isNotEmpty())
            }
        )
    }

    // -------------------------
    // UI Helpers
    // -------------------------
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun updateKickActionEnabled(enabled: Boolean) {
        binding.actionKickMembers.isEnabled = enabled
        val colorRes = if (enabled) R.color.accent_red_normal else R.color.fill_assistive
        binding.actionKickMembers.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
    }

    private fun applyRoleUi(isLeader: Boolean) {
        isLeaderUser = isLeader

        // 팀장만 더보기 메뉴 노출
        binding.toolbar.menu.findItem(R.id.action_more)?.isVisible = isLeader
        binding.toolbar.menu.findItem(R.id.action_more)?.isEnabled = isLeader

        // 킥모드 아닐 때만 위험 영역 노출
        if (!isKickMode) {
            binding.dividerDangerActions.isVisible = true
            binding.tvLeaveTeamspace.isVisible = true
            binding.tvDeleteTeamspace.isVisible = isLeader
        }
    }

    private fun enterKickMode() {
        isKickMode = true

        binding.cancelBtn.isVisible = true
        binding.actionKickMembers.isVisible = true

        // 킥모드에서는 위험영역(나가기/삭제) 숨김
        binding.dividerDangerActions.isVisible = false
        binding.tvLeaveTeamspace.isVisible = false
        binding.tvDeleteTeamspace.isVisible = false

        updateKickActionEnabled(false)
        memberAdapter.setKickMode(true)
    }

    private fun exitKickMode() {
        isKickMode = false

        binding.cancelBtn.isVisible = false
        binding.actionKickMembers.isVisible = false

        binding.dividerDangerActions.isVisible = true
        binding.tvLeaveTeamspace.isVisible = true
        binding.tvDeleteTeamspace.isVisible = isLeaderUser

        updateKickActionEnabled(false)
        memberAdapter.setKickMode(false)
    }

    private fun renderMembers(list: List<TeamMemberUi>) {
        val sorted = list.sortedByDescending { it.isLeader }
        memberAdapter.submitList(sorted)

        // 리더 1명만 있고 나머지 없으면 empty 노출
        val showEmpty = (sorted.size == 1 && sorted.first().isLeader)
        binding.emptyMember.isVisible = showEmpty
    }

    // -------------------------
    // Dialogs
    // -------------------------
    private fun showCreateTeamspaceSheet() {
        viewModel.resetCreateTeamspaceUiState()

        createTeamspaceDialog = InputDialogFragment.newInstance(
            title = getString(R.string.teamspace_create_title),
            description = getString(R.string.teamspace_create_prompt),
            hint = getString(R.string.teamspace_name_hint),
            buttonText = getString(R.string.teamspace_create_cta),
            maxLength = 20
        ).apply {
            onConfirm = { name -> viewModel.createTeamspace(name) }
        }

        createTeamspaceDialog?.show(parentFragmentManager, InputDialogFragment.TAG)
    }

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

    private fun showKickMemberDialog(memberName: String, onConfirm: () -> Unit) {
        BasicDialog.destructive(
            context = requireContext(),
            title = getString(R.string.dialog_teamspace_kick_title, memberName),
            message = getString(R.string.dialog_teamspace_kick_message),
            negativeText = getString(R.string.cancel),
            positiveText = getString(R.string.dialog_teamspace_kick_action),
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
            positiveText = getString(R.string.dialog_delete),
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

    // -------------------------
    // Collect / Observe
    // -------------------------
    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // 공통 로딩 오버레이 + 스와이프 종료
                launch {
                    viewModel.uiState.collect { state ->
                        val loading = state is UiState.Loading
                        loadingOverlay.setVisible(loading)

                        if (!loading) {
                            binding.contentRoot.isVisible = true
                            binding.swipeRefresh.isRefreshing = false
                        }
                    }
                }

                // 팀스페이스 생성 완료 이벤트
                launch {
                    viewModel.teamspaceCreatedEvent.collect { (id, name) ->
                        selectedTeamspaceId = id
                        selectedTeamspaceName = name
                        binding.tvTeamspaceTitle.text = name

                        viewModel.setTeamspaceId(id)
                        viewModel.saveLastTeamspaceId(id)

                        createTeamspaceDialog?.dismiss()
                        createTeamspaceDialog = null

                        TeamspaceInviteBottomSheet.newInstance(id)
                            .show(parentFragmentManager, "teamspace_invite")
                    }
                }

                // 리더 여부 반영
                launch {
                    viewModel.isLeader.collect { isLeader ->
                        applyRoleUi(isLeader)
                        memberAdapter.setLeaderUser(isLeader)

                        // 권한 바뀌었을 때 킥모드 정리
                        if (!isLeader) exitKickMode()
                        else if (!isKickMode) exitKickMode()
                    }
                }

                // 토스트 이벤트
                launch {
                    viewModel.toastMessage.collect { event ->
                        if (event.isErr) CustomToast.showNegative(requireContext(), event.txtRes)
                        else CustomToast.showPositive(requireContext(), event.txtRes)
                    }
                }

                // 네비 이벤트
                launch {
                    viewModel.navEvent.collect { event ->
                        when (event) {
                            NavEvent.Close -> findNavController().navigateUp()
                        }
                    }
                }

                // 멤버 렌더
                launch {
                    viewModel.members.collect { list ->
                        renderMembers(list)
                    }
                }

              // 5) 초대 링크 공유 시트
                launch {
                    viewModel.shareInviteLink.collect { url ->
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, url)
                            putExtra(Intent.EXTRA_SUBJECT, "[${selectedTeamspaceName}] 팀 초대")
                        }
                        startActivity(Intent.createChooser(intent, null))
                    }
                }
                // 팀스페이스 생성 다이얼로그 상태
                launch {
                    viewModel.createTeamspaceUiState.collect { state ->
                        val dialog = createTeamspaceDialog ?: return@collect
                        when (state) {
                            is UiState.None -> dialog.showDefault()
                            is UiState.Loading -> dialog.showLoading()
                            is UiState.Success -> {
                                dialog.showComplete()
                                viewModel.resetCreateTeamspaceUiState()
                                // dismiss는 teamspaceCreatedEvent에서 처리
                            }

                            is UiState.Error -> {
                                dialog.showDefault()
                                viewModel.resetCreateTeamspaceUiState()
                            }
                        }
                    }
                }

                // 팀스페이스 이름 변경 다이얼로그 상태
                launch {
                    viewModel.renameTeamspaceUiState.collect { state ->
                        val dialog = renameTeamspaceDialog ?: return@collect
                        when (state) {
                            is UiState.None -> dialog.showDefault()
                            is UiState.Loading -> dialog.showLoading()
                            is UiState.Success -> {
                                pendingRenameName?.let { binding.tvTeamspaceTitle.text = it }
                                pendingRenameName = null

                                dialog.showComplete()
                                delay(800)
                                dialog.dismiss()
                                renameTeamspaceDialog = null
                                viewModel.resetRenameTeamspaceUiState()
                            }

                            is UiState.Error -> {
                                dialog.showDefault()
                                viewModel.resetRenameTeamspaceUiState()
                            }
                        }
                    }
                }

                // 팀장 위임 bottom sheet 상태
                launch {
                    viewModel.transferLeaderUiState.collect { state ->
                        when (state) {
                            is UiState.Loading -> memberActionSheet?.showLeaderLoading()
                            is UiState.Success -> {
                                memberActionSheet?.showLeaderSuccessAndClose()
                                memberActionSheet = null
                                viewModel.resetTransferLeaderUiState()
                            }
                            is UiState.Error -> {
                                memberActionSheet?.showLeaderFail(state.message ?: "팀장 위임 실패")
                                viewModel.resetTransferLeaderUiState()
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }

    // -------------------------
    // Lifecycle
    // -------------------------
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentManageTeamspaceBinding.bind(view)

        // 최초 로딩: 컨텐츠 숨기고 오버레이
        binding.contentRoot.isVisible = false
        loadingOverlay.setVisible(true)

        // Home에서 넘어온 teamspaceId/name
        selectedTeamspaceId = arguments?.getString("teamspaceId").orEmpty()
        selectedTeamspaceName = arguments?.getString("teamspaceName").orEmpty()
        if (selectedTeamspaceName.isNotBlank()) binding.tvTeamspaceTitle.text = selectedTeamspaceName

        // RecyclerView
        binding.rvMembers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = memberAdapter
        }

        // Divider (1회)
        if (!dividerAdded) {
            val divider =
                MaterialDividerItemDecoration(requireContext(), RecyclerView.VERTICAL).apply {
                    setDividerColor(ContextCompat.getColor(requireContext(), R.color.stroke_strong))
                    setDividerThickness(dp(1))
                    setDividerInsetStart(dp(24))
                    setDividerInsetEnd(dp(24))
                    isLastItemDecorated = false
                }
            binding.rvMembers.addItemDecoration(divider)
            dividerAdded = true
        }

        // 상태 수집 시작
        observeState()

        // 데이터 로드
        if (selectedTeamspaceId.isNotBlank()) {
            viewModel.setTeamspaceId(selectedTeamspaceId)
            viewModel.loadMembers()
        }
        viewModel.loadTeamspaces()

        // Toolbar
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_more -> {
                    val anchor =
                        binding.toolbar.findViewById<View>(R.id.action_more) ?: binding.toolbar
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

        binding.teamspaceInviteBtn.setOnClickListener {
            viewModel.createInviteLink()
        }

        // swipe refresh
        binding.swipeRefresh.setOnRefreshListener {
            if (selectedTeamspaceId.isNotBlank()) viewModel.loadMembers()
            else binding.swipeRefresh.isRefreshing = false
        }

        // 팀스페이스 스위처
        binding.teamspaceTitleArea.setOnClickListener {
            val items = viewModel.teamspaces.value.map { ts -> TeamspaceUi(id = ts.id, name = ts.name) }

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

        // 킥모드 버튼들
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
                onNegative = {},
                onPositive = {
                    viewModel.kickMembers(selectedIds.toList())
                    exitKickMode()
                }
            ).show()
        }

        // 나가기 / 삭제
        binding.tvLeaveTeamspace.setOnClickListener {
            if (isLeaderUser) {
                showLeaderCannotLeaveDialog()
                return@setOnClickListener
            }

            val teamName = binding.tvTeamspaceTitle.text?.toString().orEmpty()
            val title = if (teamName.isNotBlank()) {
                "$teamName ${getString(R.string.dialog_teamspace_leave_teamspace_title)}"
            } else getString(R.string.dialog_teamspace_leave_teamspace_title)

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
            showDeleteTeamspaceDialog(teamName) { viewModel.deleteTeamspace() }
        }

        // (선택) 팀장 위임 결과를 받아 UI를 갱신하고 싶으면 여기서 처리
        parentFragmentManager.setFragmentResultListener(
            MemberActionBottomSheet.REQUEST_KEY_TRANSFER_OWNER,
            viewLifecycleOwner
        ) { _, _ ->
            // 예: viewModel.loadMembers()
        }
    }

    override fun onDestroyView() {
        memberActionSheet = null
        createTeamspaceDialog = null
        renameTeamspaceDialog = null
        _binding = null
        super.onDestroyView()
    }
}
