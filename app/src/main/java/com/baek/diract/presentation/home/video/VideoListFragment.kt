package com.baek.diract.presentation.home.video

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.baek.diract.presentation.home.video.upload.UploadVideoFragment
import com.baek.diract.R
import com.baek.diract.databinding.FragmentVideoListBinding
import com.baek.diract.domain.model.VideoSummary
import com.baek.diract.presentation.common.CustomToast
import com.baek.diract.presentation.common.recyclerview.SpacingItemDecoration
import com.baek.diract.presentation.common.UiState
import com.baek.diract.presentation.common.dialog.BasicDialog
import com.baek.diract.presentation.common.dialog.InputDialogFragment
import com.baek.diract.presentation.common.option.OptionItem
import com.baek.diract.presentation.common.option.OptionPopup
import com.baek.diract.presentation.home.video.move_video.MoveVideoFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class VideoListFragment : Fragment() {
    private var _binding: FragmentVideoListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VideoListViewModel by viewModels()

    private lateinit var videoCardAdapter: VideoCardAdapter
    private lateinit var sectionChipAdapter: SectionChipAdapter

    private var editNameDialog: InputDialogFragment? = null

    private var statusBarHeight = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideoListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.refreshAll()
        initView()
        initAdapter()
        observeViewModel()
    }

    private fun initView() {
        binding.titleTxt.text = viewModel.tracksTitle

        binding.backBtn.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.swipeRefreshLayoutList.setOnRefreshListener {
            viewModel.refreshAll()
        }

        binding.addVideoBtn.setOnClickListener {
            startVideoUploadFlow()
        }

        binding.uploadBtn.setOnClickListener {
            startVideoUploadFlow()
        }

        applyStatusBarInsetsToToolbar()
    }

    private fun showEditNameDialog(video: VideoSummary) {
        viewModel.resetEditUiState()

        editNameDialog = InputDialogFragment.newInstance(
            title = getString(R.string.dialog_video_name_title),
            initialText = video.title,
            hint = getString(R.string.dialog_video_name_hint),
            maxLength = 20
        ).apply {
            onConfirm = { newName ->
                viewModel.editVideoName(video.id, newName)
            }
        }
        editNameDialog?.show(parentFragmentManager, InputDialogFragment.TAG)
    }

    private fun showMoveVideoDialog(video: VideoSummary) {
        MoveVideoFragment.newInstance(
            tracksId = viewModel.tracksId,
            trackId = video.trackId,
            sectionId = video.sectionId
        ).show(childFragmentManager, MoveVideoFragment.TAG)

        childFragmentManager.setFragmentResultListener(
            MoveVideoFragment.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val movedSectionId = bundle.getString(MoveVideoFragment.MOVED_SECTION_ID)
            if (movedSectionId != null) {
                viewModel.selectSection(movedSectionId)
            }
        }
    }

    private fun startVideoUploadFlow() {
        val action = VideoListFragmentDirections.actionVideoListFragmentToUploadVideoFragment(
            viewModel.tracksTitle
        )
        findNavController().navigate(action)

        setFragmentResultListener(UploadVideoFragment.REQUEST_KEY) { _, bundle ->
            val videoUri =
                bundle.getString(UploadVideoFragment.RESULT_VIDEO_URI)?.let { Uri.parse(it) }
            val videoTitle = bundle.getString(UploadVideoFragment.RESULT_VIDEO_TITLE)
            if (videoUri != null && videoTitle != null) {
                viewModel.uploadVideo(videoUri, videoTitle)
            }
        }
    }

    private fun applyStatusBarInsetsToToolbar() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.topBar) { v, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            statusBarHeight = top
            val baseHeight = resources.getDimensionPixelSize(R.dimen.toolbar_height_64) // 64dp

            // 툴바 높이를 status bar 만큼 늘려서 "내용이 눌리지" 않게
            v.updateLayoutParams<ViewGroup.LayoutParams> {
                height = baseHeight + top
            }

            // 실제 아이콘/타이틀이 status bar 아래로 내려오게
            v.updatePadding(top = top)

            insets
        }
    }

    private fun initAdapter() {
        sectionChipAdapter = SectionChipAdapter(
            onSetSectionClick = ::onSetSectionClick,
            onSectionClick = ::onSectionClick
        )
        //아이템 간격 설정
        binding.rvSections.apply {
            adapter = sectionChipAdapter
            addItemDecoration(
                SpacingItemDecoration(
                    resources.getDimensionPixelSize(R.dimen.section_chip_spacing),
                    RecyclerView.HORIZONTAL
                )
            )
        }

        videoCardAdapter = VideoCardAdapter(
            onItemClick = ::onVideoItemClick,
            onMoreClick = ::onVideoMoreClick,
            onCancelClick = ::onVideoCancelClick,
            onRetryClick = ::onVideoRetryClick
        )
        //아이템 간격 설정
        binding.rvVideoList.apply {
            adapter = videoCardAdapter
            addItemDecoration(
                VideoCardItemDecoration(
                    spanCount = 2,
                    horizontalSpacing = resources.getDimensionPixelSize(R.dimen.video_card_spacing),
                    verticalSpacing = resources.getDimensionPixelSize(R.dimen.video_card_spacing)
                )
            )
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.videoItems.collect { items ->
                        videoCardAdapter.submitList(items)
                    }
                }
                launch {
                    viewModel.sectionChipItems.collect { items ->
                        sectionChipAdapter.submitList(items)
                        // 섹션 칩이 로드된 후 viewSpacer 높이 설정 (AppBarLayout + 상태바)
                        binding.rvSections.post {
                            binding.viewSpacer.updateLayoutParams<ViewGroup.LayoutParams> {
                                height =
                                    binding.appBarLayout.height + statusBarHeight + statusBarHeight
                            }
                        }
                    }
                }
                launch {
                    viewModel.uiState.collect { state ->
                        binding.swipeRefreshLayoutList.isRefreshing = state is UiState.Loading
                        when (state) {
                            is UiState.Error -> showError()

                            is UiState.Success -> showListOrEmpty(
                                viewModel.videoItems.value.isEmpty()
                            )

                            else -> Unit
                        }
                    }
                }
                // 이름 수정 상태 관찰
                launch {
                    viewModel.editUiState.collect { state ->
                        when (state) {
                            is UiState.None -> {
                                editNameDialog?.showDefault()
                            }

                            is UiState.Loading -> {
                                editNameDialog?.showLoading()
                            }

                            is UiState.Success -> {
                                editNameDialog?.showComplete()
                                delay(800)
                                editNameDialog?.dismiss()
                                viewModel.resetEditUiState()
                            }

                            is UiState.Error -> {
                                viewModel.resetEditUiState()
                            }
                        }
                    }
                }
                viewModel.toastMessage.collect { event ->
                    if (event.isErr) {
                        CustomToast.showNegative(requireContext(), event.txtRes, Toast.LENGTH_LONG)
                    } else {
                        CustomToast.showPositive(requireContext(), event.txtRes, Toast.LENGTH_LONG)
                    }

                }
            }
        }
    }

    //데이터 불러오기 에러 시
    private fun showError() {
        binding.errorView.visibility = View.VISIBLE
        binding.stateView.visibility = View.VISIBLE
        binding.rvVideoList.visibility = View.GONE
        binding.emptyView.visibility = View.GONE
    }

    //데이터 불러오기 성공 시
    private fun showListOrEmpty(isEmpty: Boolean) {
        binding.errorView.visibility = View.GONE
        binding.rvVideoList.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.stateView.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    // 비디오 아이템 클릭 (완료된 비디오만 클릭 가능)
    private fun onVideoItemClick(video: VideoSummary) {
        Toast.makeText(requireContext(), "영상 선택: ${video.title}", Toast.LENGTH_SHORT).show()
        // TODO: 비디오 상세 화면으로 이동
    }

    // 더보기 버튼 클릭
    private fun onVideoMoreClick(video: VideoSummary, anchor: View) {
        when (viewModel.getVideoType(video)) {
            VideoType.OTHER_USER_VIDEO -> {
                OptionPopup
                    .reportOptions(requireContext()) { option ->
                        handleVideoOption(option, video)
                    }
                    .show(anchor)
            }

            VideoType.MY_VIDEO_DEFAULT -> {
                OptionPopup
                    .videoOptions(requireContext()) { option ->
                        handleVideoOption(option, video)
                    }
                    .show(anchor)
            }

            VideoType.MY_VIDEO_NO_PART -> {
                OptionPopup
                    .basicOptions(requireContext()) { option ->
                        handleVideoOption(option, video)
                    }
                    .show(anchor)
            }
        }
    }

    private fun handleVideoOption(option: OptionItem, video: VideoSummary) {
        when (option.id) {
            OptionItem.ID_EDIT_NAME -> {
                showEditNameDialog(video)
            }

            OptionItem.ID_MOVE -> {
                showMoveVideoDialog(video)
            }

            OptionItem.ID_DELETE -> {
                // TODO: 삭제 확인 다이얼로그 표시
                BasicDialog.destructive(
                    context = requireContext(),
                    title = getString(R.string.dialog_delete_video_title, video.title),
                    message = getString(R.string.dialog_delete_video_content),
                    positiveText = getString(R.string.dialog_delete),
                    onPositive = {
                        viewModel.deleteVideo(video)
                    }
                ).show()
            }

            OptionItem.ID_REPORT -> {
                // TODO: 신고 처리
                Toast.makeText(requireContext(), "신고: ${video.title}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 실패 아이템 취소 버튼 클릭
    private fun onVideoCancelClick(item: VideoCardItem.Failed) {
        viewModel.cancelFailedItem(item)
    }

    //실패 아이템 다시 시도 버튼 클릭
    private fun onVideoRetryClick(item: VideoCardItem.Failed) {
        viewModel.retryFailedItem(item)
    }

    // 섹션 설정 버튼 클릭
    private fun onSetSectionClick() {
        val action = VideoListFragmentDirections.actionVideoListFragmentToSectionSettingFragment(
            viewModel.tracksId,
            viewModel.tracksTitle
        )
        findNavController().navigate(action)
    }

    // 섹션 칩 클릭
    private fun onSectionClick(section: SectionChipItem.SectionUi) {
        viewModel.selectSection(section.id)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
