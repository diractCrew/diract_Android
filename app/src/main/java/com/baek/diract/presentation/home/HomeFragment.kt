package com.baek.diract.presentation.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.baek.diract.R
import com.baek.diract.databinding.FragmentHomeBinding
import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.model.ProjectSummary
import com.baek.diract.domain.model.TeamspaceSummary
import com.baek.diract.domain.model.TracksSummary
import com.baek.diract.presentation.common.CustomToast
import com.baek.diract.presentation.common.UiState
import com.baek.diract.presentation.common.dialog.BasicDialog
import com.baek.diract.presentation.common.dialog.InputDialogFragment
import com.baek.diract.presentation.common.option.OptionItem
import com.baek.diract.presentation.common.option.OptionPopup
import com.baek.diract.presentation.common.recyclerview.SpacingItemDecoration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.max

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private val prefs by lazy {
        requireContext().getSharedPreferences(
            "home_onboarding",
            android.content.Context.MODE_PRIVATE
        )
    }
    private val KEY_TEAMSPACE_EMPTY_TIP_SHOWN = "teamspace_empty_tip_shown"

    private fun hasShownTeamspaceEmptyTip(): Boolean =
        prefs.getBoolean(KEY_TEAMSPACE_EMPTY_TIP_SHOWN, false)

    private fun markTeamspaceEmptyTipShown() {
        prefs.edit().putBoolean(KEY_TEAMSPACE_EMPTY_TIP_SHOWN, true).apply()
    }

    private val _teamspaces = MutableStateFlow<List<TeamspaceSummary>>(emptyList())
    val teamspaces: StateFlow<List<TeamspaceSummary>> = _teamspaces.asStateFlow()

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var deletingProjectId: String? = null
    private var deletingTracksId: String? = null
    private val loadingOverlay by lazy { com.baek.diract.presentation.common.LoadingOverlay(this) }

    private var createTeamspaceDialog: InputDialogFragment? = null
    private var createProjectDialog: InputDialogFragment? = null
    private var createTracksDialog: InputDialogFragment? = null

    private var lastTracksTargetProjectId: String? = null

    private enum class EditMode { NONE, PROJECT, TRACKS }

    private var currentEditMode = EditMode.NONE

    private val viewModel: HomeViewModel by viewModels()

    // ✅ “프로젝트별 tracks 캐시”(HomeFragment가 관리)
    private val tracksByProject = mutableMapOf<String, MutableList<TracksSummary>>()

    // 프로젝트 이름 편집 상태
    private var editingProjectId: String? = null
    private var editingDraft: String = ""

    private lateinit var projectAdapter: ProjectAdapter

    // =========================
    // 1) 상단바 모드 전환
    // =========================
    private fun setTopBarEditMode(isEditing: Boolean) = with(binding) {
        if (isEditing) {
            tvCreateProjectTitle.text = getString(R.string.project_list_title)
            homeCreateProjectBar.visibility = View.GONE
            homeProjectToolbar.visibility = View.GONE
            homeEditProjectBar.visibility = View.VISIBLE
        } else {
            tvCreateProjectTitle.text =
                viewModel.currentTeamspaceName.value.ifBlank {
                    getString(R.string.home_teamspace_current_name)
                }
            homeCreateProjectBar.visibility = View.VISIBLE
            homeProjectToolbar.visibility = View.VISIBLE
            homeEditProjectBar.visibility = View.GONE
        }
    }

    private fun showProjectEditDone(show: Boolean) = with(binding) {
        btnAddProject.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun isValidProjectName(name: String): Boolean {
        return name.trim().isNotEmpty() && name.length <= 20
    }

    private fun onClickEditDone() {
        val name = editingDraft.trim()
        if (!isValidProjectName(name)) return
        val id = editingProjectId ?: return
        viewModel.renameProject(id, name)
        exitProjectEditMode()
    }

    private fun enterProjectEditMode(projectId: String, currentName: String) {
        currentEditMode = EditMode.PROJECT
        editingProjectId = projectId
        editingDraft = currentName

        projectAdapter.setEditMode(projectId, currentName)
        showProjectEditDone(true)
        setTopBarEditMode(true)

        binding.confirmBtn.isEnabled = isValidProjectName(currentName)
        binding.confirmBtn.alpha = if (binding.confirmBtn.isEnabled) 1.0f else 0.4f
    }

    private fun exitProjectEditMode() {
        if (editingProjectId == null && currentEditMode == EditMode.NONE) return

        editingProjectId = null
        currentEditMode = EditMode.NONE
        editingDraft = ""

        projectAdapter.clearEditMode()
        setTopBarEditMode(false)
        showProjectEditDone(false)
    }

    // =========================
    // render
    // =========================
    private fun render(model: HomeUiModel) = with(binding) {
        renderHasTeamspace(model.hasTeamspace)
        if (model.hasTeamspace) {
            renderProjects(model.projects, model.tracksCountByProjectId)
        }
        if (model.isLoading && !swipeRefresh.isRefreshing) {
            CreateTeamspaceFirstLayout.visibility = View.GONE
            CreateProjectFirstLayout.visibility = View.GONE
            homeProjectToolbar.visibility = View.GONE
            emptyProject.visibility = View.GONE
            tipScrim.visibility = View.GONE
            cardProjectTip.visibility = View.GONE
            cardManageTeamspaceTip.visibility = View.GONE
            teamspaceTipScrim.visibility = View.GONE
            cardTeamspaceTip.visibility = View.GONE
            return@with
        }


        val isEmptyProjectsScreen = model.hasTeamspace && model.projects.isEmpty()
        val curStep = viewModel.projectTipStep.value ?: 2

        if (isEmptyProjectsScreen) {
            if (!viewModel.isEmptyProjectTipDone()) {
                if (curStep == 2) viewModel.setProjectTipStep(0)
            } else {
                if (curStep != 2) viewModel.setProjectTipStep(2)
            }
        } else {
            if (curStep != 2) viewModel.setProjectTipStep(2)
        }

        renderTip(viewModel.projectTipStep.value ?: 2)
    }


    // =========================
    // Fragment lifecycle
    // =========================
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadingOverlay.setVisible(true)

        // 첫 프레임 깜빡임 방지
        binding.CreateTeamspaceFirstLayout.visibility = View.GONE
        binding.CreateProjectFirstLayout.visibility = View.GONE
        binding.homeProjectToolbar.visibility = View.GONE
        binding.rvProjects.visibility = View.GONE
        binding.emptyProject.visibility = View.GONE
        binding.tipScrim.visibility = View.GONE
        binding.cardProjectTip.visibility = View.GONE
        binding.cardManageTeamspaceTip.visibility = View.GONE
        binding.teamspaceTipScrim.visibility = View.GONE
        binding.cardTeamspaceTip.visibility = View.GONE

        setupProjectRecycler()

        // ✅ 중요: 마지막 아이템이 바텀네비/제스처바/키보드 뒤로 안 들어가게 (최종)
        applyBottomInsetsToProjects()

        viewModel.projectTipStep.observe(viewLifecycleOwner) { step ->
            renderTip(step)
        }

        collectHomeUiState()
        collectTeamspaceTitle()
        collectRenameProject()
        collectDeleteProject()
        observeHomeDialogs()
        collectDeleteTracks()
        viewModel.loadHome()
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadHome()
        }
        // 클릭 리스너들
        binding.teamspaceTipScrim.setOnClickListener {
            viewModel.markNoTeamspaceTipDone()
            showTeamspaceTipOverlay(false)
            markTeamspaceEmptyTipShown()
        }

        binding.tipScrim.setOnClickListener {
            val step = viewModel.projectTipStep.value ?: 2
            advanceTipStep(step)
        }

        binding.confirmBtn.setOnClickListener {
            if (editingProjectId != null) {
                onClickEditDone()
                return@setOnClickListener
            }
            val ok = projectAdapter.commitActiveTracksEditAndExit()
            if (ok) setTopBarEditMode(false)
        }

        binding.ivEmptyProject.setOnClickListener { showCreateProjectSheet() }
        binding.btnAddProject.setOnClickListener { showCreateProjectSheet() }
        binding.CreateTeamspaceBar.setOnClickListener { showCreateTeamspaceSheet() }

        binding.manageTeamspace.setOnClickListener {
            val teamspace =
                (viewModel.homeUiState.value as? UiState.Success)?.data?.selectedTeamspace
            if (teamspace == null) return@setOnClickListener

            val bundle = Bundle().apply {
                putString("teamspaceId", teamspace.id)
                putString("teamspaceName", teamspace.name)
            }
            findNavController().navigate(
                R.id.action_homeFragment_to_manageTeamspaceFragment,
                bundle
            )
        }
    }


    private fun applyBottomInsetsToProjects() {
        val extra = resources.getDimensionPixelSize(R.dimen.spacing_12)
        binding.rvProjects.clipToPadding = false

        val bottomNav = requireActivity().findViewById<View>(R.id.bottom_nav)

        bottomNav?.post {
            val navHeight = bottomNav.height

            ViewCompat.setOnApplyWindowInsetsListener(binding.rvProjects) { v, insets ->
                val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                val bottom = (if (imeBottom > 0) imeBottom else navHeight) + extra
                v.updatePadding(bottom = bottom)
                insets
            }
            ViewCompat.requestApplyInsets(binding.rvProjects)
        }
    }


    override fun onResume() {
        super.onResume()
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
    }

    // =========================
    // collect
    // =========================
    private fun collectTeamspaceTitle() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentTeamspaceName.collect { name ->
                    if (currentEditMode == EditMode.NONE) {
                        binding.tvCreateProjectTitle.text =
                            if (name.isNotBlank()) name
                            else getString(R.string.home_teamspace_current_name)
                    }
                }
            }
        }
    }

    private fun collectRenameProject() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.renameProjectUiState.collect { state ->
                    when (state) {
                        is UiState.Success -> {
                            CustomToast.showPositive(
                                requireContext(),
                                getString(R.string.toast_project_rename_success)
                            )
                            viewModel.resetRenameProjectUiState()
                        }

                        is UiState.Error -> {
                            CustomToast.showNegative(
                                requireContext(),
                                getString(R.string.toast_project_rename_fail)
                            )
                            viewModel.resetRenameProjectUiState()
                        }

                        else -> Unit
                    }
                }
            }
        }
    }

    private fun collectDeleteProject() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.deleteProjectUiState.collect { state ->
                    when (state) {
                        is UiState.Success -> {
                            CustomToast.showPositive(
                                requireContext(),
                                getString(R.string.toast_project_delete_success)
                            )
                            viewModel.resetDeleteProjectUiState()
                        }

                        is UiState.Error -> {
                            CustomToast.showNegative(
                                requireContext(),
                                getString(R.string.toast_project_delete_fail)
                            )
                            viewModel.resetDeleteProjectUiState()
                        }

                        else -> Unit
                    }
                }
            }
        }
    }

    private fun collectHomeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.homeUiState.collect { state ->

                    // ✅ Loading 아니면 무조건 스와이프 스피너 종료
                    if (state !is UiState.Loading) {
                        binding.swipeRefresh.post {
                            binding.swipeRefresh.isRefreshing = false
                        }
                    }

                    when (state) {
                        is UiState.Loading -> {
                            val refreshing = binding.swipeRefresh.isRefreshing

                            loadingOverlay.setVisible(!refreshing)
                        }

                        is UiState.Success -> {
                            loadingOverlay.setVisible(false)
                            render(state.data)
                        }

                        is UiState.Error -> {
                            loadingOverlay.setVisible(false)
                            // 토스트 등
                        }

                        else -> Unit
                    }
                }
            }
        }
    }


    // =========================
    // dialogs
    // =========================
    private fun observeHomeDialogs() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // 팀스페이스 생성
                launch {
                    viewModel.createTeamspaceUiState.collect { state ->
                        val dialog = createTeamspaceDialog ?: return@collect
                        when (state) {
                            is UiState.Loading -> dialog.showLoading()
                            is UiState.Success -> {
                                dialog.showComplete()
                                delay(800)
                                dialog.dismissAllowingStateLoss()
                                createTeamspaceDialog = null
                                viewModel.resetCreateTeamspaceUiState()
                            }

                            is UiState.Error -> {
                                dialog.showDefault()
                                CustomToast.showNegative(
                                    requireContext(),
                                    getString(R.string.toast_project_create_fail)
                                )
                                viewModel.resetCreateTeamspaceUiState()
                            }

                            else -> Unit
                        }
                    }
                }

                // 프로젝트 생성
                launch {
                    viewModel.createProjectUiState.collect { state ->
                        val dialog = createProjectDialog ?: return@collect
                        when (state) {
                            is UiState.Loading -> dialog.showLoading()
                            is UiState.Success -> {
                                dialog.showComplete()
                                delay(800)
                                dialog.dismissAllowingStateLoss()
                                createProjectDialog = null
                                viewModel.resetCreateProjectUiState()
                            }

                            is UiState.Error -> {
                                dialog.showDefault()
                                viewModel.resetCreateProjectUiState()
                            }

                            else -> Unit
                        }
                    }
                }

                // 트랙 추가
                launch {
                    viewModel.createTracksUiState.collect { state ->
                        val dialog = createTracksDialog ?: return@collect
                        when (state) {
                            is UiState.Loading -> dialog.showLoading()
                            is UiState.Success -> {
                                val projectId = lastTracksTargetProjectId
                                if (!projectId.isNullOrBlank()) {
                                    when (val list = viewModel.getTracksListOnce(projectId)) {
                                        is DataResult.Success -> {
                                            tracksByProject[projectId] = list.data.toMutableList()
                                            projectAdapter.setTracks(projectId, list.data)
                                        }

                                        else -> Unit
                                    }
                                    lastTracksTargetProjectId = null
                                }

                                dialog.showComplete()
                                delay(800)
                                dialog.dismissAllowingStateLoss()
                                createTracksDialog = null
                                viewModel.resetCreateTracksUiState()
                            }

                            is UiState.Error -> {
                                dialog.showDefault()
                                viewModel.resetCreateTracksUiState()
                            }

                            else -> Unit
                        }
                    }
                }
            }
        }
    }

    // =========================
    // Recycler setup
    // =========================
    private fun setupProjectRecycler() {
        projectAdapter = ProjectAdapter(
            onLongClick = { v, p -> showProjectActions(anchor = v, project = p) },
            onClick = { /* 필요 시 */ },
            onEditTextChanged = { draft ->
                editingDraft = draft
                val isValid = isValidProjectName(draft)
                binding.confirmBtn.isEnabled = isValid
                binding.confirmBtn.alpha = if (isValid) 1.0f else 0.4f
            },

            onAddTracks = { project -> showAddTracksSheet(project) },

            onOpenTracks = { _, project ->
                // ✅ 펼치자마자 로딩 UI로 통일 (버벅임 줄어듦)
                projectAdapter.setTracksLoading(project.id, true)

                viewLifecycleOwner.lifecycleScope.launch {
                    when (val res = viewModel.getTracksListOnce(project.id)) {
                        is DataResult.Success -> {
                            tracksByProject[project.id] = res.data.toMutableList()
                            projectAdapter.setTracks(project.id, res.data) // SUCCESS 상태로 바뀜
                        }

                        is DataResult.Error -> {
                            projectAdapter.setTracksError(project.id) // 실패 UI
                        }
                    }
                }
            },

            onRetryTracks = { project ->
                projectAdapter.setTracksLoading(project.id, true)

                viewLifecycleOwner.lifecycleScope.launch {
                    when (val res = viewModel.getTracksListOnce(project.id)) {
                        is DataResult.Success -> {
                            tracksByProject[project.id] = res.data.toMutableList()
                            projectAdapter.setTracks(project.id, res.data)
                        }

                        is DataResult.Error -> {
                            projectAdapter.setTracksError(project.id)
                        }
                    }
                }
            },

            onTracksClick = { _, tracksSummary ->
                val action = HomeFragmentDirections.actionHomeFragmentToVideoNavGraph(
                    tracksId = tracksSummary.tracksId,
                    tracksTitle = tracksSummary.trackName
                )
                findNavController().navigate(action)
            },

            onTracksDelete = { project, track ->
                BasicDialog.destructive(
                    context = requireContext(),
                    title = getString(R.string.dialog_song_delete_title, track.trackName),
                    message = getString(R.string.dialog_song_cannot_recover),
                    positiveText = getString(R.string.dialog_delete),
                    onPositive = {
                        deletingProjectId = project.id
                        deletingTracksId = track.tracksId
                        viewModel.deleteTracks(project.id, track.tracksId) // ✅ 서버 요청
                    }
                ).show()
            },

            onTracksRename = { project, track, newName ->
                val list = tracksByProject[project.id] ?: mutableListOf()
                val idx = list.indexOfFirst { it.tracksId == track.tracksId }
                if (idx != -1) {
                    list[idx] = list[idx].copy(trackName = newName)
                    tracksByProject[project.id] = list
                    projectAdapter.setTracks(project.id, list.toList())
                }
                viewModel.renameTracks(project.id, track.tracksId, newName)
            },

            onTracksEditStateChanged = { isEditing ->
                currentEditMode = if (isEditing) EditMode.TRACKS else EditMode.NONE
                setTopBarEditMode(isEditing)
            }
        )

        binding.rvProjects.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = projectAdapter
            clipToPadding = false
            itemAnimator = null
            if (itemDecorationCount == 0) {
                addItemDecoration(
                    SpacingItemDecoration(
                        spacing = resources.getDimensionPixelSize(R.dimen.spacing_12),
                        orientation = RecyclerView.VERTICAL,
                        includeEdge = false
                    )
                )
            }
        }
    }

    // =========================
    // teamspace / projects render
    // =========================
    private fun showTeamspaceTipOverlay(show: Boolean) = with(binding) {
        teamspaceTipScrim.visibility = if (show) View.VISIBLE else View.GONE
        cardTeamspaceTip.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun renderHasTeamspace(hasTeamspace: Boolean) {
        binding.CreateTeamspaceFirstLayout.visibility =
            if (hasTeamspace) View.GONE else View.VISIBLE
        binding.CreateProjectFirstLayout.visibility = if (hasTeamspace) View.VISIBLE else View.GONE

        val shouldShowTip = !hasTeamspace && !hasShownTeamspaceEmptyTip()
        showTeamspaceTipOverlay(shouldShowTip)
    }

    private fun renderProjects(projects: List<ProjectSummary>, counts: Map<String, Int>) {
        val isEmpty = projects.isEmpty()

        binding.homeProjectToolbar.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.emptyProject.visibility = if (isEmpty) View.VISIBLE else View.GONE

        // ✅ 리스트 있으면 swipeRefresh를 보여야 함
        binding.swipeRefresh.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.rvProjects.visibility = View.VISIBLE // 안쪽은 항상 visible로 두는 게 안전

        if (!isEmpty) {
            projectAdapter.setTracksCount(counts)
            projectAdapter.submitList(projects)
        }
    }

    // =========================
    // tips
    // =========================
    private fun renderTip(step: Int) = with(binding) {
        val show = step == 0 || step == 1
        tipScrim.visibility = if (show) View.VISIBLE else View.GONE
        cardProjectTip.visibility = if (step == 0) View.VISIBLE else View.GONE
        cardManageTeamspaceTip.visibility = if (step == 1) View.VISIBLE else View.GONE

        if (show) {
            tipScrim.bringToFront()
            cardProjectTip.bringToFront()
            cardManageTeamspaceTip.bringToFront()
        }
    }

    private fun advanceTipStep(current: Int) {
        val next = when (current) {
            0 -> 1
            1 -> 2
            else -> 2
        }
        viewModel.setProjectTipStep(next)
        if (next == 2) viewModel.markEmptyProjectTipDone()
    }

    // =========================
    // create dialogs
    // =========================
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

    private fun showCreateProjectSheet() {
        viewModel.resetCreateProjectUiState()
        createProjectDialog = InputDialogFragment.newInstance(
            title = getString(R.string.project_create_title),
            description = getString(R.string.project_create_prompt),
            hint = getString(R.string.project_name_hint),
            buttonText = getString(R.string.project_create_cta),
            maxLength = 20
        ).apply {
            onConfirm = { name -> viewModel.createProject(name) }
        }
        createProjectDialog?.show(parentFragmentManager, InputDialogFragment.TAG)
    }

    private fun collectDeleteTracks() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.deleteTracksUiState.collect { state ->

                    val pendingProjectId = deletingProjectId
                    val pendingTracksId = deletingTracksId

                    when (state) {
                        is UiState.Loading -> {
                            if (!pendingProjectId.isNullOrBlank()) {
                                projectAdapter.setTracksLoading(pendingProjectId, true)
                            }
                        }

                        is UiState.Success -> {
                            val (pId, tId) = state.data

                            // ✅ 스크롤 튐 방지: 상태 저장
                            val lm = binding.rvProjects.layoutManager as? LinearLayoutManager
                            val rvState = lm?.onSaveInstanceState()

                            // 로딩 끄기
                            projectAdapter.setTracksLoading(pId, false)

                            // 캐시 제거 + adapter 주입
                            val list = tracksByProject[pId] ?: mutableListOf()
                            val newList = list.filterNot { it.tracksId == tId }.toMutableList()
                            tracksByProject[pId] = newList
                            projectAdapter.setTracks(pId, newList)

                            // ✅ 상태 복구 (레이아웃 반영 후)
                            binding.rvProjects.post {
                                lm?.onRestoreInstanceState(rvState)
                            }

                            CustomToast.showPositive(requireContext(), "곡이 삭제되었습니다.")

                            deletingProjectId = null
                            deletingTracksId = null
                            viewModel.resetDeleteTracksUiState()
                        }

                        is UiState.Error -> {
                            if (!pendingProjectId.isNullOrBlank()) {
                                projectAdapter.setTracksLoading(pendingProjectId, false)
                            }

                            CustomToast.showNegative(requireContext(), "곡 삭제를 실패했습니다.")

                            deletingProjectId = null
                            deletingTracksId = null
                            viewModel.resetDeleteTracksUiState()
                        }

                        else -> Unit
                    }
                }
            }
        }
    }


    private fun showAddTracksSheet(project: ProjectSummary) {
        viewModel.resetCreateTracksUiState()

        createTracksDialog = InputDialogFragment.newInstance(
            title = getString(R.string.song_add_title),
            description = getString(R.string.song_add_prompt),
            hint = getString(R.string.song_name_hint),
            buttonText = getString(R.string.song_add_cta),
            maxLength = 20
        ).apply {
            onConfirm = { name ->
                lastTracksTargetProjectId = project.id
                viewModel.createTracks(project.id, name)
            }
        }

        createTracksDialog?.show(parentFragmentManager, InputDialogFragment.TAG)
    }

    // =========================
    // options
    // =========================
    private fun showProjectActions(anchor: View, project: ProjectSummary) {
        OptionPopup
            .basicOptions(
                context = requireContext(),
                onOptionSelected = { option ->
                    when (option.id) {
                        OptionItem.ID_EDIT_NAME -> enterProjectEditMode(project.id, project.name)
                        OptionItem.ID_DELETE -> showDeleteDialog(project)
                    }
                }
            )
            .show(anchor)
    }

    private fun showDeleteDialog(project: ProjectSummary) {
        BasicDialog.destructive(
            context = requireContext(),
            title = "${project.name} 프로젝트를 삭제하시겠어요?",
            message = "프로젝트 모든 내용이 삭제됩니다.",
            positiveText = "삭제",
            onPositive = { viewModel.deleteProject(project.id) }
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        createTeamspaceDialog = null
        createProjectDialog = null
        createTracksDialog = null
        _binding = null
    }
}
