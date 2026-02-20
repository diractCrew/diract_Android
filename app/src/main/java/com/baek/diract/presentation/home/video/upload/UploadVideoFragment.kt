package com.baek.diract.presentation.home.video.upload

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.baek.diract.R
import com.baek.diract.presentation.common.recyclerview.GridSpacingItemDecoration
import com.baek.diract.databinding.FragmentUploadVideoBinding
import com.baek.diract.presentation.common.CustomToast
import com.baek.diract.presentation.common.UiState
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UploadVideoFragment : BottomSheetDialogFragment() {

    companion object {
        const val REQUEST_KEY = "upload_video_request"
        const val RESULT_VIDEO_URI = "video_uri"
        const val RESULT_VIDEO_TITLE = "video_title"
    }

    private var _binding: FragmentUploadVideoBinding? = null
    private val binding get() = _binding!!

    private val args: UploadVideoFragmentArgs by navArgs()
    private val viewModel: UploadVideoViewModel by viewModels()

    private lateinit var galleryVideoAdapter: GalleryVideoAdapter

    private var player: ExoPlayer? = null

    private var hasShownMaxLengthToast = false
    private var navigatedToSettings = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.loadGalleryVideos()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // 선택한 것만 허용 → 부분 접근으로 로드
            viewModel.loadGalleryVideos()
            binding.partialAccessBanner.visibility = View.VISIBLE
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                it.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                behavior.isDraggable = false
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUploadVideoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
        initAdapter()
        observeViewModel()
        checkPermissionAndLoadVideos()

        setupTouchOutsideToHideKeyboard(view)
    }

    private fun checkPermissionAndLoadVideos() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                when {
                    ContextCompat.checkSelfPermission(
                        requireContext(), Manifest.permission.READ_MEDIA_VIDEO
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        viewModel.loadGalleryVideos()
                    }

                    ContextCompat.checkSelfPermission(
                        requireContext(), Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        // 선택한 것만 허용 상태 → 부분 접근으로 로드
                        viewModel.loadGalleryVideos()
                        binding.partialAccessBanner.visibility = View.VISIBLE
                    }

                    else -> requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_VIDEO)
                }
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                if (ContextCompat.checkSelfPermission(
                        requireContext(), Manifest.permission.READ_MEDIA_VIDEO
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    viewModel.loadGalleryVideos()
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_VIDEO)
                }
            }

            else -> {
                if (ContextCompat.checkSelfPermission(
                        requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    viewModel.loadGalleryVideos()
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
    }

    private fun initView() {
        binding.subTitleTxt.text = args.tracksTitle
        binding.closeBtn.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.uploadBtn.setOnClickListener {
            handleUpload()
        }

        // 필터 탭 리스너
        binding.filterTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val filter = when (tab?.position) {
                    0 -> VideoFilter.ALL
                    1 -> VideoFilter.FAVORITE
                    else -> VideoFilter.ALL
                }
                viewModel.setFilter(filter)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        setupTitleInput()

        binding.allowAllBtn.setOnClickListener {
            navigatedToSettings = true
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", requireContext().packageName, null)
            }
            startActivity(intent)
        }
    }

    // 설정 화면에서 돌아왔을 때 권한 상태 재확인 및 영상 재로드
    override fun onResume() {
        super.onResume()
        if (navigatedToSettings) {
            navigatedToSettings = false
            binding.partialAccessBanner.visibility = View.GONE
            checkPermissionAndLoadVideos()
        }
    }

    private fun initAdapter() {
        galleryVideoAdapter = GalleryVideoAdapter(
            onItemClick = { item ->
                binding.scrollView.fullScroll(ScrollView.FOCUS_UP);
                viewModel.selectVideo(item)
            }
        )

        val spacingPx = (1 * resources.displayMetrics.density).toInt()

        binding.rvGallery.apply {
            adapter = galleryVideoAdapter
            layoutManager = GridLayoutManager(requireContext(), 4)
            addItemDecoration(GridSpacingItemDecoration(4, spacingPx, false))
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.galleryState.collect { state ->

                        binding.galleryLoadingView.visibility =
                            if (state is UiState.Loading) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.filteredVideos.collect { videos ->
                        updateGalleryView(videos)
                    }
                }
                launch {
                    viewModel.selectedVideo.collect { videoItem ->
                        updateView(videoItem)
                    }
                }
            }
        }
    }

    private fun updateGalleryView(videos: List<GalleryVideoItem>) {
        val isEmpty = videos.isEmpty()
        binding.rvGallery.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.galleryEmptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        galleryVideoAdapter.submitList(videos)
    }

    private fun updateView(item: GalleryVideoItem?) {
        binding.uploadBtn.isEnabled = item != null
        binding.titleInputTxt.isEnabled = item != null
        binding.titleInputTxt.hint =
            getString(if (item == null) R.string.upload_video_hint_disabled else R.string.upload_video_hint_enabled)
        updatePlayerView(item)
    }

    private fun updatePlayerView(item: GalleryVideoItem?) {
        binding.emptyPlayView.visibility = if (item == null) View.VISIBLE else View.GONE
        binding.playerView.visibility = if (item != null) View.VISIBLE else View.GONE

        if (item == null) {
            releasePlayer()
            return
        }
        if (player == null) {
            player = ExoPlayer.Builder(requireContext()).build()
            binding.playerView.player = player
        }
        val mediaItem = MediaItem.fromUri(item.uri)

        player?.apply {
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = false
        }
    }

    private fun handleUpload() {
        val selectedVideo = viewModel.selectedVideo.value ?: return
        val title = binding.titleInputTxt.text?.toString()?.trim()

        if (!viewModel.canUpload(title)) {
            binding.titleInputTxt.requestFocus()
            return
        }

        setFragmentResult(
            REQUEST_KEY,
            bundleOf(
                RESULT_VIDEO_URI to selectedVideo.uri.toString(),
                RESULT_VIDEO_TITLE to title
            )
        )
        findNavController().navigateUp()
    }

    private fun releasePlayer() {
        player?.release()
        player = null
        binding.playerView.player = null
    }

    private fun setupTitleInput() {
        // 20자 제한
        val maxLength = 20
        binding.titleInputTxt.filters = arrayOf(InputFilter.LengthFilter(maxLength))

        // Focus 리스너
        setupTitleFocusListener(maxLength)

        // 텍스트 변경 리스너
        setupTitleTextChangedListener(maxLength)
        // 키보드 완료 버튼 클릭
        setupTitleEditorAction()

        // 클리어 버튼
        binding.clearBtn.setOnClickListener {
            binding.titleInputTxt.text?.clear()
        }
    }

    // ️ 포커스 리스너
    private fun setupTitleFocusListener(maxLength: Int) {
        binding.titleInputTxt.setOnFocusChangeListener { _, hasFocus ->
            val length = binding.titleInputTxt.text?.length ?: 0
            updateInputContainerBackground(hasFocus, length, maxLength)

            binding.counterTxt.visibility = if (hasFocus) View.VISIBLE else View.GONE
            binding.clearBtn.visibility = if (hasFocus && length != 0) View.VISIBLE else View.GONE
        }
    }

    // 텍스트 변경 리스너
    private fun setupTitleTextChangedListener(maxLength: Int) {
        binding.titleInputTxt.doAfterTextChanged { text ->
            val length = text?.length ?: 0
            val hasFocus = binding.titleInputTxt.hasFocus()

            updateInputContainerBackground(hasFocus, length, maxLength)

            // 카운터 표시
            if (length > 0) {
                binding.counterTxt.visibility = View.VISIBLE
                binding.counterTxt.text =
                    getString(R.string.title_counter_format, length, maxLength)
                binding.clearBtn.visibility = View.VISIBLE
            } else {
                binding.counterTxt.visibility = View.GONE
                binding.clearBtn.visibility = View.GONE
            }

            // 최대 길이 도달 시 토스트
            val counterColorRes =
                if (length >= maxLength) R.color.accent_red_strong else R.color.secondary_normal
            binding.counterTxt.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    counterColorRes
                )
            )

            if (length >= maxLength && !hasShownMaxLengthToast) {
                hasShownMaxLengthToast = true
                CustomToast.showPositive(
                    requireContext(),
                    getString(R.string.toast_section_name_max_length, maxLength),
                    Toast.LENGTH_LONG
                )
            } else if (length < maxLength) {
                hasShownMaxLengthToast = false
            }
        }
    }


    // 키보드 완료 버튼 처리
    private fun setupTitleEditorAction() {
        binding.titleInputTxt.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideAndClearKeyboard(v)
                true
            } else false
        }
    }

    private fun hideAndClearKeyboard(view: View) {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
        view.clearFocus()
    }

    private fun updateInputContainerBackground(hasFocus: Boolean, length: Int, maxLength: Int) {
        val backgroundRes = when {
            length >= maxLength -> R.drawable.bg_input_error
            hasFocus -> R.drawable.bg_input_focus
            else -> R.drawable.bg_input_default
        }
        binding.inputContainer.setBackgroundResource(backgroundRes)
    }

    private fun setupTouchOutsideToHideKeyboard(rootView: View) {
        // rootView를 기준으로 터치 이벤트 처리
        rootView.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val focusedView = dialog?.currentFocus
                if (focusedView is EditText) {
                    // 키보드 숨기고 포커스 제거
                    hideAndClearKeyboard(focusedView)
                }
            }
            false
        }

        // RecyclerView, 다른 터치 가능한 뷰에서도 이벤트가 전달되도록
        if (rootView is ViewGroup) {
            for (i in 0 until rootView.childCount) {
                val child = rootView.getChildAt(i)
                setupTouchOutsideToHideKeyboard(child)
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        releasePlayer()
        _binding = null
    }
}
