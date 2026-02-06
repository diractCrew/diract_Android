package com.baek.diract.presentation.home.video.player

import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import com.baek.diract.R
import com.baek.diract.databinding.FragmentVideoPlayerBinding
import com.baek.diract.presentation.common.Formatter.toTimeString
import com.baek.diract.domain.model.FeedbackUser
import com.baek.diract.presentation.common.CustomToast
import com.baek.diract.presentation.common.UiState
import com.baek.diract.presentation.common.dialog.BasicDialog
import com.baek.diract.presentation.common.option.OptionItem
import com.baek.diract.presentation.common.option.OptionPopup
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class VideoPlayerFragment : Fragment() {

    private var _binding: FragmentVideoPlayerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VideoPlayerViewModel by viewModels()
    private val feedbackViewModel: FeedbackViewModel by viewModels()

    private var player: ExoPlayer? = null

    private var isFullscreen = false
    private var isControllerVisible = false
    private var isPanelOpen = false

    private var hideControllerJob: Job? = null
    private var progressUpdateJob: Job? = null
    private var rewindIndicatorJob: Job? = null
    private var forwardIndicatorJob: Job? = null

    private var isSeekBarTracking = false
    private var systemBarInset = 0

    private lateinit var feedbackAdapter: FeedbackAdapter
    private lateinit var mentionListAdapter: MentionListAdapter

    private val backPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            when {
                isFullscreen -> toggleFullscreen()
                feedbackViewModel.feedbackInputState.value != FeedbackInputState.DEFAULT -> confirmDiscardAndReset()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideoPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        observeViewModel()
        setupControllerToggle()
        setupControllerAction()
        setupFullscreenButton()
        setupPanelButton()
        setupBackPressedCallback()
    }

    private fun initView() {
        binding.toolbar.setNavigationOnClickListener {
            if (isFullscreen) {
                toggleFullscreen()
            } else {
                findNavController().navigateUp()
            }
        }
        // 재시도 버튼
        binding.retryBtn.setOnClickListener {
            viewModel.loadVideo()
        }
        binding.toolbar.title = viewModel.videoTitle
        binding.toolbar.subtitle = viewModel.tracksTitle

        // 하단 네비게이션 바 높이만큼 feedbackView 내부 요소에 패딩 적용
        applyNavigationBarPadding()

        // FeedbackAdapter 초기화 및 RecyclerView 연결
        setupFeedbackRecyclerView()
    }

    private fun setupFeedbackRecyclerView() {
        feedbackAdapter = FeedbackAdapter(
            onMoreClick = { feedback, view ->
                showFeedbackOptionPopup(feedback, view)
            },
            onReplyClick = { feedback ->
                // TODO: 답글 화면으로 이동
            },
            onTimeChipClick = { feedback ->
                // 해당 시간으로 이동 (초 → 밀리초 변환)
                player?.let { exoPlayer ->
                    exoPlayer.seekTo((feedback.startTime * 1000).toLong())
                    exoPlayer.pause()
                }
            },
            onRetryClick = { feedback ->
                feedbackViewModel.retryUploadFeedback(feedback)
            },
            onCancelClick = { feedback ->
                feedbackViewModel.cancelFailedFeedback(feedback.feedbackId)
            },
            onMoreMentionClick = { feedback, view ->
                showMentionMorePopup(feedback, view)
            }
        )
        binding.feedbackView.rvComment.adapter = feedbackAdapter

        // 피드백 리스트 터치 시 멘션 리스트 숨기기 + 키보드 내리기
        binding.feedbackView.rvComment.addOnItemTouchListener(
            object : RecyclerView.SimpleOnItemTouchListener() {
                override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                    if (e.action == MotionEvent.ACTION_DOWN) {
                        dismissMentionAndKeyboard()
                    }
                    return false
                }
            }
        )

        // 헤더 영역 터치 시 멘션 리스트 숨기기 + 키보드 내리기
        binding.feedbackView.header.setOnClickListener {
            dismissMentionAndKeyboard()
        }

        // 필터 칩 클릭 리스너
        binding.feedbackView.chipFilterFeedback.setOnCheckedChangeListener { _, isChecked ->
            feedbackViewModel.filterFeedback(isChecked)
        }

        // SwipeRefreshLayout 새로고침 리스너
        binding.feedbackView.swipeRefreshLayoutList.setOnRefreshListener {
            feedbackViewModel.loadFeedbacks()
        }

        // 시점 피드백 버튼
        binding.feedbackView.pointFeedbackBtn.setOnClickListener {
            feedbackViewModel.startPointFeedback(player?.currentPosition ?: 0L)
        }

        // 구간 피드백 버튼
        binding.feedbackView.intervalFeedbackBtn.setOnClickListener {
            feedbackViewModel.startIntervalFeedback(player?.currentPosition ?: 0L)
        }

        // 구간 선택 완료 버튼
        binding.feedbackView.selectingRangeBtn.setOnClickListener {
            feedbackViewModel.completeRangeSelection(player?.currentPosition ?: feedbackViewModel.rangeStartTime)
        }

        // 댓글 시트 닫기 버튼
        binding.feedbackView.commentSheet.closeBtn.setOnClickListener {
            confirmDiscardAndReset()
        }

        // 멘션 리스트 어댑터 설정
        setupMentionList()

        // 입력 텍스트에 따라 sendBtn 활성화/비활성화 및 멘션 감지
        binding.feedbackView.commentSheet.sendBtn.isEnabled = false
        binding.feedbackView.commentSheet.commentEditTxt.addTextChangedListener(
            object : android.text.TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    handleMentionInput(s, start, before, count)
                }

                override fun afterTextChanged(s: android.text.Editable?) {
                    binding.feedbackView.commentSheet.sendBtn.isEnabled = !s.isNullOrBlank()
                }
            }
        )

        // 피드백 전송 버튼
        binding.feedbackView.commentSheet.sendBtn.setOnClickListener {
            val content = binding.feedbackView.commentSheet.commentEditTxt.text.toString().trim()
            if (content.isEmpty()) return@setOnClickListener
            feedbackViewModel.submitFeedback(content)
            binding.feedbackView.rvComment.scrollToPosition(0)
        }
    }


    private fun confirmDiscardAndReset() {
        val hasContent = !binding.feedbackView.commentSheet.commentEditTxt.text.isNullOrBlank()
        if (hasContent) {
            BasicDialog.destructive(
                context = requireContext(),
                title = getString(R.string.dialog_cancel_writing_feedback_title),
                message = getString(R.string.dialog_cancel_writing_feedback_content),
                positiveText = getString(R.string.dialog_exit),
                onPositive = { feedbackViewModel.resetFeedbackInput() }
            ).show()
        } else {
            feedbackViewModel.resetFeedbackInput()
        }
    }

    private fun renderFeedbackInputState(state: FeedbackInputState) {
        backPressedCallback.isEnabled = isFullscreen || state != FeedbackInputState.DEFAULT

        when (state) {
            FeedbackInputState.DEFAULT -> {
                binding.feedbackView.defaultActionView.visibility = View.VISIBLE
                binding.feedbackView.selectingRangeBtn.visibility = View.GONE
                binding.feedbackView.commentSheet.root.visibility = View.GONE
                hideKeyboard()
                binding.feedbackView.commentSheet.commentEditTxt.text?.clear()
                binding.feedbackView.commentSheet.tagContainer.visibility = View.GONE
                binding.feedbackView.commentSheet.mentionChipGroup.removeAllViews()
                hideMentionList()
                if (!isFullscreen) {
                    showToolbar()
                }
            }

            FeedbackInputState.SELECTING_RANGE -> {
                binding.feedbackView.defaultActionView.visibility = View.GONE
                binding.feedbackView.selectingRangeBtn.visibility = View.VISIBLE
                binding.feedbackView.commentSheet.root.visibility = View.GONE
                binding.feedbackView.rangeStartTxt.text = feedbackViewModel.rangeStartTime.toTimeString()
                binding.feedbackView.rangeEndTxt.text = feedbackViewModel.rangeStartTime.toTimeString()
                hideKeyboard()
            }

            FeedbackInputState.WRITING_COMMENT -> {
                binding.feedbackView.defaultActionView.visibility = View.GONE
                binding.feedbackView.selectingRangeBtn.visibility = View.GONE
                binding.feedbackView.commentSheet.root.visibility = View.VISIBLE
                binding.feedbackView.commentSheet.commentHeader.visibility = View.VISIBLE
                binding.feedbackView.commentSheet.chipContainer.visibility = View.VISIBLE
                // 시간 칩 텍스트 설정
                val startTime = feedbackViewModel.rangeStartTime
                val endTime = feedbackViewModel.rangeEndTime
                binding.feedbackView.commentSheet.timeChipTxt.text = if (endTime != null) {
                    "${startTime.toTimeString()} ~ ${endTime.toTimeString()}"
                } else {
                    startTime.toTimeString()
                }
                // 수정 모드일 때 기존 내용 및 멘션 칩 설정
                feedbackViewModel.editingFeedback?.let { editing ->
                    binding.feedbackView.commentSheet.commentEditTxt.setText(editing.content)
                    bindEditMentionChips(editing)
                }
                showKeyboard()
            }
        }
    }

    private fun showKeyboard() {
        binding.feedbackView.commentSheet.commentEditTxt.requestFocus()
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(
            binding.feedbackView.commentSheet.commentEditTxt,
            InputMethodManager.SHOW_IMPLICIT
        )
    }

    private fun hideKeyboard() {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    private fun hideToolbar() {
        binding.toolbar.visibility = View.GONE
    }

    private fun showToolbar() {
        binding.toolbar.visibility = View.VISIBLE
    }

    private fun showMentionMorePopup(feedback: FeedbackItem, anchorView: View) {
        val popupView = layoutInflater.inflate(R.layout.popup_mention, null)
        val minWidthPx = resources.getDimensionPixelSize(R.dimen.mention_popup_min_width)
        popupView.minimumWidth = minWidthPx
        val paddingPx = resources.getDimensionPixelSize(R.dimen.mention_popup_vert_padding)
        popupView.setPadding(0, paddingPx, 0, paddingPx)

        val rv = popupView.findViewById<RecyclerView>(R.id.mentionListRv)
        val adapter = MentionListAdapter { }
        rv.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        adapter.submitList(feedback.taggedUsers)
        rv.adapter = adapter

        // 5개 초과 시 높이 제한
        val maxVisibleItems = 4
        val itemHeight = resources.getDimensionPixelSize(R.dimen.mention_item_height)
        if (feedback.taggedUsers.size > maxVisibleItems) {
            rv.layoutParams.height = itemHeight * maxVisibleItems - itemHeight/4
        }

        val popupWindow = android.widget.PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow.elevation = 8f
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // 뷰 오른쪽 끝에서 margin만큼 떨어지도록 배치
        popupView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val popupWidth = popupView.measuredWidth
        val margin = resources.getDimensionPixelSize(R.dimen.mention_list_horiz_margin)
        val anchorLocation = IntArray(2)
        anchorView.getLocationOnScreen(anchorLocation)
        val screenWidth = resources.displayMetrics.widthPixels
        val xOffset = screenWidth - anchorLocation[0] - popupWidth - margin
        popupWindow.showAsDropDown(anchorView, xOffset, paddingPx)
    }

    private fun showFeedbackOptionPopup(feedback: FeedbackItem, anchorView: View) {
        val popup = if (feedbackViewModel.isMyFeedback(feedback)) {
            OptionPopup.feedbackAuthorOptions(requireContext()) { option ->
                onFeedbackOptionSelected(option, feedback)
            }
        } else {
            OptionPopup.feedbackOptions(requireContext()) { option ->
                onFeedbackOptionSelected(option, feedback)
            }
        }
        popup.show(anchorView)
    }

    private fun onFeedbackOptionSelected(option: OptionItem, feedback: FeedbackItem) {
        when (option.id) {
            OptionItem.ID_EDIT_FEEDBACK -> {
                feedbackViewModel.startEditFeedback(feedback)
                // 이미 WRITING_COMMENT 상태일 경우 StateFlow가 재방출하지 않으므로 수동 렌더링
                renderFeedbackInputState(FeedbackInputState.WRITING_COMMENT)
            }
            OptionItem.ID_DELETE -> {
                feedbackViewModel.deleteFeedback(feedback.feedbackId)
            }
            OptionItem.ID_REPORT -> {
                feedbackViewModel.reportFeedback(feedback.feedbackId)
            }
        }
    }

    private fun bindEditMentionChips(feedback: FeedbackItem) {
        // startEditFeedback에서 이미 selectedMentions에 추가했으므로 렌더링만 수행
        renderMentionChips()
    }

    private fun setupMentionList() {
        mentionListAdapter = MentionListAdapter { member ->
            onMentionSelected(member)
        }
        binding.mentionListRv.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
            adapter = mentionListAdapter
        }
    }

    private fun handleMentionInput(s: CharSequence?, start: Int, before: Int, count: Int) {
        val text = s?.toString() ?: ""

        // '@' 입력 시 멘션 모드 시작
        if (count == 1 && text.endsWith("@")) {
            feedbackViewModel.setMentioning(true)
            showMentionList("")
            return
        }

        // 멘션 모드가 아니면 무시
        if (!feedbackViewModel.isMentioning) return

        // 텍스트 끝에서 마지막 '@' 찾기
        val lastAtIndex = text.lastIndexOf('@')
        if (lastAtIndex == -1) {
            hideMentionList()
            feedbackViewModel.setMentioning(false)
            return
        }

        // '@' 이후의 쿼리 추출
        val query = text.substring(lastAtIndex + 1)

        // 띄어쓰기가 포함되면 멘션 모드 종료
        if (query.contains(" ")) {
            hideMentionList()
            feedbackViewModel.setMentioning(false)
            return
        }

        showMentionList(query)
    }

    private fun showMentionList(query: String) {
        val filteredMembers = feedbackViewModel.filterMentionMembers(query)
        val maxVisibleItems = 5
        val itemHeight = resources.getDimensionPixelSize(R.dimen.mention_item_height)

        if (filteredMembers.isEmpty()) {
            binding.mentionListRv.visibility = View.GONE
            binding.mentionEmptyTxt.visibility = View.VISIBLE
        } else {
            binding.mentionListRv.visibility = View.VISIBLE
            binding.mentionEmptyTxt.visibility = View.GONE
            mentionListAdapter.submitList(filteredMembers)

            // 5개 이상이면 높이 제한
            val layoutParams = binding.mentionListRv.layoutParams
            layoutParams.height = if (filteredMembers.size > maxVisibleItems) {
                itemHeight * maxVisibleItems - itemHeight/4
            } else {
                ViewGroup.LayoutParams.WRAP_CONTENT
            }
            binding.mentionListRv.layoutParams = layoutParams
        }

        binding.mentionListContainer.visibility = View.VISIBLE
        updateMentionListPosition()
    }

    private fun hideMentionList() {
        binding.mentionListContainer.visibility = View.GONE
    }

    // 멘션 리스트 숨기기 + 키보드 내리기 (외부 영역 터치 시)
    private fun dismissMentionAndKeyboard() {
        if (feedbackViewModel.feedbackInputState.value != FeedbackInputState.WRITING_COMMENT) return
        if (feedbackViewModel.isMentioning) {
            feedbackViewModel.setMentioning(false)
            hideMentionList()
        }
        hideKeyboard()
    }

    private fun updateMentionListPosition() {
        // commentSheet 위에 배치 (commentSheet 높이 + 여백)
        binding.feedbackView.commentSheet.root.post {
            val commentSheetHeight = binding.feedbackView.commentSheet.root.height
            val margin = resources.getDimensionPixelSize(R.dimen.mention_list_margin)
            val horizontalMargin = resources.getDimensionPixelSize(R.dimen.mention_list_horiz_margin)

            val params = binding.mentionListContainer.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            params.bottomMargin = commentSheetHeight + margin

            // feedbackView 기준으로 너비와 위치 조정
            val feedbackView = binding.feedbackView.root
            params.width = feedbackView.width - (horizontalMargin * 2)
            params.marginStart = feedbackView.left + horizontalMargin
            params.marginEnd = horizontalMargin

            binding.mentionListContainer.layoutParams = params
        }
    }

    private fun onMentionSelected(member: FeedbackUser) {
        val editText = binding.feedbackView.commentSheet.commentEditTxt
        val text = editText.text ?: return

        // 마지막 '@'부터 끝까지 삭제
        val lastAtIndex = text.lastIndexOf('@')
        if (lastAtIndex != -1) {
            text.delete(lastAtIndex, text.length)
        }

        // 멘션 칩 추가
        addMentionChip(member)

        // 상태 초기화
        feedbackViewModel.setMentioning(false)
        hideMentionList()
    }

    private fun addMentionChip(member: FeedbackUser) {
        feedbackViewModel.addMention(member)
        renderMentionChips()
    }

    private fun renderMentionChips() {
        val tagContainer = binding.feedbackView.commentSheet.tagContainer
        val mentionChipGroup = binding.feedbackView.commentSheet.mentionChipGroup
        mentionChipGroup.removeAllViews()

        val selected = feedbackViewModel.selectedMentions.value
        if (selected.isEmpty()) {
            tagContainer.visibility = View.GONE
            return
        }

        tagContainer.visibility = View.VISIBLE
        mentionChipGroup.visibility = View.VISIBLE

        if (feedbackViewModel.isAllMembersSelected()) {
            // 전체 멤버가 선택된 경우 @All 칩 하나만 표시
            val chip = layoutInflater.inflate(
                R.layout.item_added_mention_chip, mentionChipGroup, false
            ) as Chip
            chip.text = "@All"
            chip.tag = FeedbackViewModel.ALL_MEMBER_ID
            chip.setOnCloseIconClickListener {
                feedbackViewModel.clearMentions()
                renderMentionChips()
            }
            mentionChipGroup.addView(chip)
        } else {
            // 개별 멤버 칩 표시
            selected.forEach { user ->
                val chip = layoutInflater.inflate(
                    R.layout.item_added_mention_chip, mentionChipGroup, false
                ) as Chip
                chip.text = "@${user.name}"
                chip.tag = user.userId
                chip.setOnCloseIconClickListener {
                    feedbackViewModel.removeMention(user.userId ?: return@setOnCloseIconClickListener)
                    renderMentionChips()
                }
                mentionChipGroup.addView(chip)
            }
        }
    }

    private fun applyNavigationBarPadding() {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val navBarBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val isKeyboardVisible = imeBottom > 0

            // 전체화면이 아니면서 WRITING_COMMENT 상태일 때 키보드에 따라 toolbar 표시/숨김
            if (!isFullscreen && feedbackViewModel.feedbackInputState.value == FeedbackInputState.WRITING_COMMENT) {
                if (isKeyboardVisible) {
                    hideToolbar()
                } else {
                    showToolbar()
                }
                // 멘션 모드일 때 위치 업데이트
                if (feedbackViewModel.isMentioning) {
                    updateMentionListPosition()
                }
            }

            // actionContainer 하단 패딩 적용
            val actionContainer = binding.feedbackView.actionContainer
            actionContainer.setPadding(
                actionContainer.paddingLeft,
                actionContainer.paddingTop,
                actionContainer.paddingRight,
                resources.getDimensionPixelSize(R.dimen.action_container_vert_padding) + navBarBottom
            )

            // commentSheet: 키보드가 올라오면 키보드 위에, 아니면 네비게이션 바 위에
            val commentSheet = binding.feedbackView.commentSheet.root
            val bottomPadding = if (isFullscreen) {
                // 전체화면
                (if (imeBottom > 0) imeBottom else 0)
            } else {
                // 일반 모드: 키보드 또는 네비게이션 바 높이
                (if (imeBottom > 0) imeBottom else navBarBottom)
            }
            commentSheet.setPadding(
                commentSheet.paddingLeft,
                commentSheet.paddingTop,
                commentSheet.paddingRight,
                resources.getDimensionPixelSize(R.dimen.comment_sheet_padding) + bottomPadding
            )

            insets
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 비디오 상태 관찰
                launch {
                    viewModel.videoState.collect { state ->
                        updateVideoState(state)
                    }
                }
                // 비디오 정보 관찰
                launch {
                    viewModel.videoInfo.collect { info ->
                        if (info == null) return@collect
                        binding.toolbar.title = info.videoTitle
                    }
                }
                // 피드백 상태 관찰
                launch {
                    feedbackViewModel.feedbackState.collect { state ->
                        updateFeedbackState(state)
                    }
                }
                // 피드백 입력 상태 관찰
                launch {
                    feedbackViewModel.feedbackInputState.collect { state ->
                        renderFeedbackInputState(state)
                    }
                }
                // 피드백 목록 관찰
                launch {
                    feedbackViewModel.feedbackItem.collect { feedbacks ->
                        feedbackAdapter.submitList(feedbacks)
                        // 성공 상태에서 빈 목록일 때만 emptyView 표시
                        val isSuccess = feedbackViewModel.feedbackState.value is UiState.Success
                        binding.feedbackView.emptyView.visibility =
                            if (isSuccess && feedbacks.isEmpty()) View.VISIBLE else View.GONE
                    }
                }

                //토스트 메세지
                feedbackViewModel.toastMessage.collect { event ->
                    if (event.isErr) {
                        CustomToast.showNegative(requireContext(), event.txtRes, Toast.LENGTH_LONG)
                    } else {
                        CustomToast.showPositive(requireContext(), event.txtRes, Toast.LENGTH_LONG)
                    }
                }
            }
        }
    }

    private fun updateVideoState(state: VideoPlayerState) {
        // 모든 뷰 숨김
        binding.loadingView.visibility = View.GONE
        binding.downloadingView.visibility = View.GONE
        binding.videoErrorView.visibility = View.GONE
        binding.playerView.visibility = View.GONE

        when (state) {
            is VideoPlayerState.Initial -> Unit // 초기 상태 - 아무것도 표시하지 않음

            is VideoPlayerState.Loading -> {
                binding.loadingView.visibility = View.VISIBLE
            }

            is VideoPlayerState.Downloading -> {
                binding.downloadingView.visibility = View.VISIBLE
                binding.progressBar.progress = state.progress
                binding.progressText.text = state.progress.toString()
            }

            is VideoPlayerState.Ready -> {
                binding.playerView.visibility = View.VISIBLE
                initializePlayer(state.videoUri.toString())
            }

            is VideoPlayerState.Error -> {
                binding.videoErrorView.visibility = View.VISIBLE
            }
        }
    }

    private fun updateFeedbackState(state: UiState<Long>) {
        binding.feedbackView.swipeRefreshLayoutList.isRefreshing = state is UiState.Loading
        binding.feedbackView.errorView.visibility = View.GONE
        binding.feedbackView.emptyView.visibility = View.GONE

        when (state) {
            is UiState.None -> Unit
            is UiState.Loading -> Unit
            is UiState.Success -> {
                binding.feedbackView.rvComment.visibility = View.VISIBLE
            }

            is UiState.Error -> {
                binding.feedbackView.rvComment.visibility = View.GONE
                binding.feedbackView.errorView.visibility = View.VISIBLE
            }
        }
    }

    private fun initializePlayer(videoUri: String) {
        if (player == null) {
            player = ExoPlayer.Builder(requireContext()).build().apply {
                binding.playerView.player = this
                setMediaItem(MediaItem.fromUri(videoUri))
                // ViewModel에서 저장된 재생 위치로 복원
                seekTo(viewModel.playbackPosition)
                playWhenReady = viewModel.playWhenReady
                prepare()

                // 플레이어 상태 리스너
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            // duration 설정
                            val duration = this@apply.duration
                            binding.videoController.durationTxt.text = duration.toTimeString()
                            binding.videoController.seekBar.max = duration.toInt()
                            // 진행률 업데이트 시작
                            startProgressUpdate()
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        // 재생/일시정지 아이콘 변경
                        binding.videoController.btnPlayPause.setImageResource(
                            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
                        )
                    }
                })
            }
            setupSeekBar()
        }
    }

    private fun setupSeekBar() {
        binding.videoController.seekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.videoController.curTimeTxt.text = progress.toLong().toTimeString()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isSeekBarTracking = true
                resetHideTimer()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isSeekBarTracking = false
                player?.seekTo(seekBar?.progress?.toLong() ?: 0L)
                resetHideTimer()
            }
        })
    }

    private fun startProgressUpdate() {
        progressUpdateJob?.cancel()
        progressUpdateJob = viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                updateProgress()
                delay(PROGRESS_UPDATE_INTERVAL)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressUpdateJob?.cancel()
    }

    private fun updateProgress() {
        player?.let {
            val position = it.currentPosition
            if (!isSeekBarTracking) {
                binding.videoController.curTimeTxt.text = position.toTimeString()
                binding.videoController.seekBar.progress = position.toInt()
            }
            // 구간 선택 중일 때 rangeEndTxt 업데이트
            if (feedbackViewModel.feedbackInputState.value == FeedbackInputState.SELECTING_RANGE) {
                binding.feedbackView.rangeEndTxt.text = position.toTimeString()
            }
        }
    }

    private fun releasePlayer() {
        stopProgressUpdate()
        player?.let {
            // 현재 재생 상태를 ViewModel에 저장
            viewModel.savePlaybackState(it.currentPosition, it.playWhenReady)
            it.release()
        }
        player = null
    }


    /*
        컨트롤러 설정 및 화면 회전 설정
     */

    private fun setupControllerToggle() {
        // 컨트롤러 영역 클릭 시 컨트롤러 토글
        binding.videoController.root.setOnClickListener {
            toggleController()
        }

        // 더블탭 제스처 설정 (싱글탭: 컨트롤러 토글, 더블탭: rewind/forward)
        setupDoubleTapGesture()
    }

    private fun setupControllerAction() {
        binding.videoController.btnSpeed.setOnClickListener {
            //TODO: 속도조절 다이얼로그 띄우기
            resetHideTimer()
        }

        binding.videoController.btnRewind.setOnClickListener {
            rewind()
            resetHideTimer()
        }

        binding.videoController.btnPlayPause.setOnClickListener {
            togglePlayPause()
            resetHideTimer()
        }

        binding.videoController.btnForward.setOnClickListener {
            forward()
            resetHideTimer()
        }
    }

    private fun togglePlayPause() {
        player?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                // 재생이 끝난 상태면 처음부터 재생
                if (it.playbackState == Player.STATE_ENDED) {
                    it.seekTo(0)
                }
                it.play()
            }
        }
    }

    private fun rewind() {
        player?.let {
            val newPosition = (it.currentPosition - SEEK_INCREMENT).coerceAtLeast(0)
            it.seekTo(newPosition)
        }
    }

    private fun forward() {
        player?.let {
            val newPosition = (it.currentPosition + SEEK_INCREMENT).coerceAtMost(it.duration)
            it.seekTo(newPosition)
        }
    }

    private fun showSeekIndicator(isForward: Boolean) {
        if (_binding == null) return

        val indicatorView = if (isForward) binding.forwardImg else binding.rewindImg

        // 기존 애니메이션 및 Job 취소
        indicatorView.animate().cancel()
        if (isForward) {
            forwardIndicatorJob?.cancel()
        } else {
            rewindIndicatorJob?.cancel()
        }

        // Fade In 애니메이션 후 Fade Out
        val job = viewLifecycleOwner.lifecycleScope.launch {
            indicatorView.alpha = 0f
            indicatorView.visibility = View.VISIBLE
            indicatorView.animate()
                .alpha(1f)
                .setDuration(SEEK_INDICATOR_FADE_DURATION)
                .start()

            delay(SEEK_INDICATOR_FADE_DURATION + SEEK_INDICATOR_DURATION)

            if (_binding == null) return@launch
            indicatorView.animate()
                .alpha(0f)
                .setDuration(SEEK_INDICATOR_FADE_DURATION)
                .withEndAction {
                    _binding?.let {
                        indicatorView.visibility = View.GONE
                    }
                }
                .start()
        }

        if (isForward) {
            forwardIndicatorJob = job
        } else {
            rewindIndicatorJob = job
        }
    }

    private fun setupDoubleTapGesture() {
        val gestureDetector = android.view.GestureDetector(
            requireContext(),
            object : android.view.GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: android.view.MotionEvent): Boolean {
                    if (_binding == null) return false
                    val viewWidth = binding.playerView.width
                    val tapX = e.x

                    if (tapX < viewWidth / 2) {
                        // 왼쪽 더블탭 - rewind
                        rewind()
                        showSeekIndicator(isForward = false)
                    } else {
                        // 오른쪽 더블탭 - forward
                        forward()
                        showSeekIndicator(isForward = true)
                    }
                    return true
                }

                override fun onSingleTapConfirmed(e: android.view.MotionEvent): Boolean {
                    if (_binding == null) return false
                    dismissMentionAndKeyboard()
                    toggleController()
                    return true
                }
            }
        )

        binding.playerView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun toggleController() {
        if (isControllerVisible) {
            hideController()
        } else {
            showController()
        }
    }

    private fun showController() {
        if (isControllerVisible) return
        if (_binding == null) return

        isControllerVisible = true
        binding.videoController.root.apply {
            visibility = View.VISIBLE
            alpha = 0f
            animate()
                .alpha(1f)
                .setDuration(CONTROLLER_ANIM_DURATION)
                .setListener(null)
                .start()
        }

        resetHideTimer()
    }

    private fun hideController() {
        if (!isControllerVisible) return
        if (_binding == null) return

        isControllerVisible = false
        hideControllerJob?.cancel()

        binding.videoController.root.animate()
            .alpha(0f)
            .setDuration(CONTROLLER_ANIM_DURATION)
            .withEndAction {
                _binding?.videoController?.root?.visibility = View.GONE
            }
            .start()
    }

    private fun resetHideTimer() {
        hideControllerJob?.cancel()
        hideControllerJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(CONTROLLER_HIDE_DELAY)
            hideController()
        }
    }

    private fun setupBackPressedCallback() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            backPressedCallback
        )
    }

    private fun setupFullscreenButton() {
        binding.videoController.btnFullscreen.setOnClickListener {
            resetHideTimer()
            toggleFullscreen()
        }
    }

    private fun setupPanelButton() {
        binding.videoController.openPanelBtn.setOnClickListener {
            resetHideTimer()
            togglePanel()
        }
    }

    private fun togglePanel() {
        isPanelOpen = !isPanelOpen
        if (isPanelOpen) {
            openPanel()
        } else {
            closePanel()
        }
    }

    private fun openPanel() {
        val constraintSet = ConstraintSet()
        constraintSet.clone(binding.root)

        val panelWidthPx = resources.getDimensionPixelSize(R.dimen.fullscreen_panel_width)

        // 피드백 뷰: 고정 dp 너비로 오른쪽에 배치 + 우측 마진
        constraintSet.clear(binding.feedbackView.root.id, ConstraintSet.START)
        constraintSet.connect(
            binding.feedbackView.root.id, ConstraintSet.END,
            ConstraintSet.PARENT_ID, ConstraintSet.END
        )
        constraintSet.constrainWidth(binding.feedbackView.root.id, panelWidthPx)
        constraintSet.setMargin(binding.feedbackView.root.id, ConstraintSet.END, systemBarInset)

        // 플레이어: 피드백 뷰 왼쪽까지 나머지 영역 차지
        // 기존 방식 (% 기반)
        // constraintSet.clear(binding.playerContainer.id, ConstraintSet.END)
        // constraintSet.constrainPercentWidth(binding.playerContainer.id, PLAYER_WIDTH_WITH_PANEL)
        constraintSet.connect(
            binding.playerContainer.id, ConstraintSet.END,
            binding.feedbackView.root.id, ConstraintSet.START
        )
        constraintSet.constrainWidth(binding.playerContainer.id, ConstraintSet.MATCH_CONSTRAINT)
        constraintSet.constrainPercentWidth(binding.playerContainer.id, 0f)  // percent 해제

        // 애니메이션 적용
        val transition = ChangeBounds().apply { duration = 300 }
        TransitionManager.beginDelayedTransition(binding.root, transition)
        constraintSet.applyTo(binding.root)

        // 아이콘 변경
        binding.videoController.openPanelBtn.setImageResource(R.drawable.ic_expand_circle_right)
    }

    private fun closePanel() {
        val constraintSet = ConstraintSet()
        constraintSet.clone(binding.root)

        val panelWidthPx = resources.getDimensionPixelSize(R.dimen.fullscreen_panel_width)

        // 플레이어: 전체 영역 차지
        constraintSet.connect(
            binding.playerContainer.id, ConstraintSet.END,
            ConstraintSet.PARENT_ID, ConstraintSet.END
        )
        constraintSet.constrainWidth(binding.playerContainer.id, ConstraintSet.MATCH_CONSTRAINT)

        // 피드백 뷰: 화면 밖 오른쪽으로 이동 (고정 너비 + 우측 마진 유지)
        constraintSet.clear(binding.feedbackView.root.id, ConstraintSet.END)
        constraintSet.connect(
            binding.feedbackView.root.id, ConstraintSet.START,
            ConstraintSet.PARENT_ID, ConstraintSet.END
        )
        constraintSet.constrainWidth(binding.feedbackView.root.id, panelWidthPx)
        constraintSet.setMargin(binding.feedbackView.root.id, ConstraintSet.END, systemBarInset)

        // 애니메이션 적용
        val transition = ChangeBounds().apply { duration = 300 }
        TransitionManager.beginDelayedTransition(binding.root, transition)
        constraintSet.applyTo(binding.root)

        // 아이콘 변경
        binding.videoController.openPanelBtn.setImageResource(R.drawable.ic_expand_circle_left)
    }

    private fun toggleFullscreen() {
        hideKeyboard()
        isFullscreen = !isFullscreen
        if (isFullscreen) {
            enterFullscreen()
        } else {
            exitFullscreen()
        }
    }

    private fun enterFullscreen() {
        val activity = requireActivity()

        // 백 버튼 콜백 활성화
        backPressedCallback.isEnabled = true

        // 가로 모드로 전환
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        // 시스템 UI 숨김 (Immersive Mode)
        val window = activity.window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // 시스템 바 영역만큼 inset 값 계산 및 저장
        val rootInsets = androidx.core.view.ViewCompat.getRootWindowInsets(binding.root)
        rootInsets?.let {
            val navBar = it.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars())
            systemBarInset = maxOf(navBar.left, navBar.right)
        }

        // root 패딩 제거를 위한 리스너 설정
        applySystemBarInsets()

        // 툴바 숨김
        binding.toolbar.visibility = View.GONE
        binding.videoController.openPanelBtn.visibility = View.VISIBLE

        // ConstraintSet으로 전체화면 레이아웃 설정
        val constraintSet = ConstraintSet()
        constraintSet.clone(binding.root)

        // 플레이어 컨테이너: 전체화면 (100%)
        constraintSet.connect(
            binding.playerContainer.id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP
        )
        constraintSet.connect(
            binding.playerContainer.id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM
        )
        constraintSet.connect(
            binding.playerContainer.id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START
        )
        constraintSet.connect(
            binding.playerContainer.id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END
        )
        constraintSet.constrainWidth(binding.playerContainer.id, ConstraintSet.MATCH_CONSTRAINT)
        constraintSet.constrainHeight(binding.playerContainer.id, ConstraintSet.MATCH_CONSTRAINT)
        constraintSet.setDimensionRatio(binding.playerContainer.id, null)
        constraintSet.constrainPercentWidth(binding.playerContainer.id, 1f)

        // 피드백 뷰: 화면 밖 오른쪽에 배치 (고정 dp 너비 + 우측 마진)
        val panelWidthPx = resources.getDimensionPixelSize(R.dimen.fullscreen_panel_width)
        constraintSet.clear(binding.feedbackView.root.id)
        constraintSet.connect(
            binding.feedbackView.root.id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP
        )
        constraintSet.connect(
            binding.feedbackView.root.id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM
        )
        constraintSet.connect(
            binding.feedbackView.root.id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END
        )
        constraintSet.constrainWidth(binding.feedbackView.root.id, panelWidthPx)
        constraintSet.constrainHeight(binding.feedbackView.root.id, ConstraintSet.MATCH_CONSTRAINT)
        constraintSet.setMargin(binding.feedbackView.root.id, ConstraintSet.END, systemBarInset)

        constraintSet.applyTo(binding.root)

        // 전체화면 종료 아이콘으로 변경
        binding.videoController.btnFullscreen.setImageResource(R.drawable.ic_fullscreen_exit)
    }

    private fun exitFullscreen() {
        val activity = requireActivity()

        // 패널이 열려있으면 닫기
        if (isPanelOpen) {
            isPanelOpen = false
        }

        // 피드백 입력 중이면 백 버튼 콜백 유지
        backPressedCallback.isEnabled =
            feedbackViewModel.feedbackInputState.value != FeedbackInputState.DEFAULT

        // 세로 모드로 복귀
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // 시스템 UI 복원
        val window = activity.window
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            show(WindowInsetsCompat.Type.systemBars())
        }

        // 시스템 바 inset 제거
        clearSystemBarInsets()

        // 툴바 복원
        binding.toolbar.visibility = View.VISIBLE

        // openPanelBtn 숨김
        binding.videoController.openPanelBtn.visibility = View.GONE
        binding.videoController.openPanelBtn.setImageResource(R.drawable.ic_expand_circle_left)

        // ConstraintSet으로 세로 모드 레이아웃 복원
        val constraintSet = ConstraintSet()
        constraintSet.clone(binding.root)

        // 플레이어 컨테이너: 고정 height로 복원
        val playerHeightPx = resources.getDimensionPixelSize(R.dimen.player_container_height)
        constraintSet.clear(binding.playerContainer.id)
        constraintSet.connect(
            binding.playerContainer.id,
            ConstraintSet.TOP,
            binding.toolbar.id,
            ConstraintSet.BOTTOM
        )
        constraintSet.connect(
            binding.playerContainer.id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START
        )
        constraintSet.connect(
            binding.playerContainer.id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END
        )
        constraintSet.constrainWidth(binding.playerContainer.id, ConstraintSet.MATCH_CONSTRAINT)
        constraintSet.constrainHeight(binding.playerContainer.id, playerHeightPx)

        // 피드백 뷰: 플레이어 아래에 배치
        constraintSet.clear(binding.feedbackView.root.id)
        constraintSet.connect(
            binding.feedbackView.root.id,
            ConstraintSet.TOP,
            binding.playerContainer.id,
            ConstraintSet.BOTTOM
        )
        constraintSet.connect(
            binding.feedbackView.root.id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM
        )
        constraintSet.connect(
            binding.feedbackView.root.id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START
        )
        constraintSet.connect(
            binding.feedbackView.root.id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END
        )
        constraintSet.constrainWidth(binding.feedbackView.root.id, ConstraintSet.MATCH_CONSTRAINT)
        constraintSet.constrainHeight(binding.feedbackView.root.id, ConstraintSet.MATCH_CONSTRAINT)

        constraintSet.applyTo(binding.root)

        // 전체화면 아이콘으로 변경
        binding.videoController.btnFullscreen.setImageResource(R.drawable.ic_fullscreen)
    }

    private fun applySystemBarInsets() {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val navBar =
                insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars())
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            systemBarInset = maxOf(navBar.left, navBar.right)

            // 전체화면 모드에서는 actionContainer 패딩을 기본값으로 복원
            val baseVertPadding =
                resources.getDimensionPixelSize(R.dimen.action_container_vert_padding)
            val baseHorizPadding =
                resources.getDimensionPixelSize(R.dimen.action_container_horiz_padding)
            val actionContainer = binding.feedbackView.actionContainer
            actionContainer.setPadding(
                baseHorizPadding,
                baseVertPadding,
                baseHorizPadding,
                baseVertPadding
            )

            // commentSheet: 키보드가 올라오면 키보드 위에 배치
            val commentSheet = binding.feedbackView.commentSheet.root
            val basePadding = resources.getDimensionPixelSize(R.dimen.comment_sheet_padding)
            commentSheet.setPadding(
                commentSheet.paddingLeft,
                commentSheet.paddingTop,
                commentSheet.paddingRight,
                basePadding + imeBottom
            )

            // 멘션 모드일 때 위치 업데이트
            if (feedbackViewModel.isMentioning) {
                updateMentionListPosition()
            }

            insets
        }
        binding.root.requestApplyInsets()
    }

    private fun clearSystemBarInsets() {
        // 인셋 값 초기화
        systemBarInset = 0

        // 세로 모드용 하단 네비게이션 바 패딩 리스너 복원
        applyNavigationBarPadding()
        binding.root.requestApplyInsets()
    }

    override fun onPause() {
        super.onPause()
        player?.let {
            // 현재 재생 상태를 ViewModel에 저장
            viewModel.savePlaybackState(it.currentPosition, it.playWhenReady)
            it.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        // ViewModel에 저장된 playWhenReady 상태에 따라 재생
        if (viewModel.playWhenReady) {
            player?.play()
        }
    }

    override fun onDestroyView() {
        // 애니메이션 취소 (Job은 lifecycleScope에 의해 자동 취소)
        binding.rewindImg.animate().cancel()
        binding.forwardImg.animate().cancel()
        releasePlayer()
        if (isFullscreen) {
            exitFullscreen()
        }
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val CONTROLLER_HIDE_DELAY = 4000L
        private const val CONTROLLER_ANIM_DURATION = 200L
        private const val PROGRESS_UPDATE_INTERVAL = 200L // 0.2초마다 업데이트
        private const val SEEK_INCREMENT = 5000L // 5초
        private const val SEEK_INDICATOR_DURATION = 400L // 아이콘 표시 유지 시간
        private const val SEEK_INDICATOR_FADE_DURATION = 300L // 페이드 애니메이션 시간
    }
}
