package com.baek.diract.presentation.home.video.player

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
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
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.fragment.findNavController
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import com.baek.diract.R
import com.baek.diract.databinding.FragmentVideoPlayerBinding
import com.baek.diract.presentation.common.Formatter.toTimeString
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@UnstableApi
@AndroidEntryPoint
class VideoPlayerFragment : Fragment() {

    private var _binding: FragmentVideoPlayerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VideoPlayerViewModel by viewModels()

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

    private val backPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            if (isFullscreen) {
                toggleFullscreen()
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
            viewModel.retry()
        }
        binding.toolbar.title = viewModel.videoTitle
        binding.toolbar.subtitle = viewModel.tracksTitle

        // 하단 네비게이션 바 높이만큼 feedbackView 내부 요소에 패딩 적용
        applyNavigationBarPadding()
    }

    private fun applyNavigationBarPadding() {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val navBarBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom

            // actionContainer 하단 패딩 적용
            val actionContainer = binding.feedbackView.actionContainer
            actionContainer.setPadding(
                actionContainer.paddingLeft,
                actionContainer.paddingTop,
                actionContainer.paddingRight,
                resources.getDimensionPixelSize(R.dimen.action_container_padding) + navBarBottom
            )

            // commentSheet 하단 패딩 적용
            val commentSheet = binding.feedbackView.commentSheet.root
            commentSheet.setPadding(
                commentSheet.paddingLeft,
                commentSheet.paddingTop,
                commentSheet.paddingRight,
                navBarBottom
            )

            insets
        }
    }

    private fun observeViewModel() {
        // 비디오 상태 관찰
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.videoState.collect { state ->
                        updateVideoState(state)
                    }
                }
                launch {
                    viewModel.videoInfo.collect { info ->
                        if (info == null) return@collect
                        binding.toolbar.title = info.videoTitle
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
            if (!isSeekBarTracking) {
                val position = it.currentPosition
                binding.videoController.curTimeTxt.text = position.toTimeString()
                binding.videoController.seekBar.progress = position.toInt()
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

        // 백 버튼 콜백 비활성화
        backPressedCallback.isEnabled = false

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
            systemBarInset = maxOf(navBar.left, navBar.right)

            // 전체화면 모드에서는 actionContainer, commentSheet 패딩을 기본값으로 복원
            val basePadding = resources.getDimensionPixelSize(R.dimen.action_container_padding)
            val actionContainer = binding.feedbackView.actionContainer
            actionContainer.setPadding(basePadding, basePadding, basePadding, basePadding)

            val commentSheet = binding.feedbackView.commentSheet.root
            commentSheet.setPadding(
                commentSheet.paddingLeft,
                commentSheet.paddingTop,
                commentSheet.paddingRight,
                0
            )

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
        private const val INSET_ANIM_DURATION = 250L // 패딩 애니메이션 시간
    }
}
