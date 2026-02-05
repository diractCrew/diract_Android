package com.baek.diract.presentation.home.video.move_video

import android.app.Dialog
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.baek.diract.R
import com.baek.diract.databinding.FragmentMoveVideoBinding
import com.baek.diract.presentation.common.CustomToast
import com.baek.diract.presentation.common.LoadingOverlay
import com.baek.diract.presentation.common.UiState
import com.baek.diract.presentation.common.dialog.BasicDialog
import com.baek.diract.presentation.common.recyclerview.SpacingItemDecoration
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MoveVideoFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentMoveVideoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MoveVideoViewModel by viewModels()

    private lateinit var adapter : MoveToSectionAdapter
    private val loadingOverlay by lazy { LoadingOverlay(this) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                handleBackPress()
                true
            } else {
                false
            }
        }
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)

        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoveVideoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFullScreen()
        initAdapter()
        setupViews()
        observeViewModel()
    }

    private fun setupFullScreen() {
        (dialog as? BottomSheetDialog)?.setOnShowListener {
            val bottomSheet = (dialog as BottomSheetDialog).findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let { sheet ->
                val behavior = BottomSheetBehavior.from(sheet)
                sheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                behavior.isDraggable = false
            }
        }
    }

    private fun initAdapter() {
        adapter = MoveToSectionAdapter { section ->
            viewModel.selectSection(section)
        }

        binding.rvPartList.apply {
            adapter = this@MoveVideoFragment.adapter
            addItemDecoration(
                SpacingItemDecoration(
                    resources.getDimensionPixelSize(R.dimen.move_video_item_spacing),
                    RecyclerView.VERTICAL
                )
            )
        }
    }

    private fun setupViews() {
        binding.closeBtn.setOnClickListener {
            handleBackPress()
        }

        binding.confirmBtn.setOnClickListener {
            viewModel.moveVideo()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { observeUiState() }
                launch { observeSections() }
                launch { observeSelectedSection() }
                launch { observeMoveState() }

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

    private suspend fun observeUiState() {
        viewModel.loadState.collect { state ->
            loadingOverlay.setVisible(state is UiState.Loading)
            when (state) {
                is UiState.Error -> {
                    dismiss()
                }

                else -> Unit
            }
        }
    }

    private suspend fun observeSections() {
        viewModel.sections.collect { sections ->
            adapter.submitList(sections)
        }
    }

    private suspend fun observeSelectedSection() {
        viewModel.selectedSectionId.collect { selectedId ->
            adapter.setSelectedSection(selectedId)
            binding.confirmBtn.isEnabled = viewModel.isMoved
        }
    }

    private suspend fun observeMoveState() {
        viewModel.moveState.collect { state ->
            when (state) {
                is UiState.None -> showDefaultState()
                is UiState.Loading -> showLoadingState()
                is UiState.Success -> {
                    setFragmentResult(REQUEST_KEY, bundleOf(MOVED_SECTION_ID to state.data))
                    dismiss()
                }

                is UiState.Error -> {
                    showDefaultState()
                }
            }
        }
    }

    private fun showDefaultState() {
        binding.confirmBtn.isVisible = true
        binding.loadingView.isVisible = false
        binding.blockingView.isVisible = false
    }

    private fun showLoadingState() {
        binding.confirmBtn.isVisible = false
        binding.loadingView.isVisible = true
        binding.blockingView.isVisible = true
    }

    private fun handleBackPress() {
        BasicDialog.destructive(
            context = requireContext(),
            title = getString(R.string.dialog_discard_title),
            message = getString(R.string.dialog_discard_message),
            positiveText = getString(R.string.dialog_exit),
            onPositive = { dismiss() }
        ).show()

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "MoveVideoFragment"
        const val REQUEST_KEY = "move_video_request"
        const val MOVED_SECTION_ID = "moved_section_id"

        fun newInstance(
            tracksId: String,
            trackId: String,
            sectionId: String
        ): MoveVideoFragment {
            return MoveVideoFragment().apply {
                arguments = bundleOf(
                    MoveVideoViewModel.KEY_TRACKS_ID to tracksId,
                    MoveVideoViewModel.KEY_TRACK_ID to trackId,
                    MoveVideoViewModel.KEY_SECTION_ID to sectionId
                )
            }
        }
    }
}
