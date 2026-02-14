package com.baek.diract.presentation.report

/**
 *
 * ReportDialogFragment.newInstance(
 *     contentType = ReportType.FEEDBACK,
 *     targetId = feedbackId,
 *     reportedId = authorId
 * ).show(childFragmentManager, ReportDialogFragment.TAG)
 */

import android.app.Dialog
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.baek.diract.databinding.FragmentReportDialogBinding
import com.baek.diract.presentation.common.CustomToast
import com.baek.diract.presentation.common.MaxLengthInputFilter
import com.baek.diract.presentation.common.UiState
import com.baek.diract.presentation.common.dialog.BasicDialog
import com.google.android.material.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReportDialogFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentReportDialogBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReportViewModel by viewModels()

    private var isError = false

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

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<FrameLayout>(R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                sheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                BottomSheetBehavior.from(sheet).apply {
                    state = BottomSheetBehavior.STATE_EXPANDED
                    skipCollapsed = true
                    isDraggable = false
                }
            }
        }

        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupInput()
        setupConfirmButton()
        observeViewModel()
        setupWindowInsets()
        setupTouchOutsideToDismissKeyboard()
    }

    private fun setupToolbar() {
        binding.closeBtn.setOnClickListener { handleBackPress() }
    }

    private fun handleBackPress() {
        val hasInput = !binding.inputTxt.text.isNullOrEmpty()
        if (hasInput) {
            BasicDialog.destructive(
                context = requireContext(),
                title = getString(com.baek.diract.R.string.report_discard_title),
                message = getString(com.baek.diract.R.string.report_discard_message),
                positiveText = getString(com.baek.diract.R.string.dialog_exit),
                onPositive = { dismiss() }
            ).show()
        } else {
            dismiss()
        }
    }

    private fun setupInput() {
        binding.inputTxt.filters = arrayOf(
            MaxLengthInputFilter(MAX_LENGTH) { showError() }
        )

        binding.inputTxt.doAfterTextChanged { text ->
            val length = text?.length ?: 0

            if (isError && length < MAX_LENGTH) {
                clearError()
            }

            binding.countTxt.text =
                getString(com.baek.diract.R.string.input_dialog_counter, length, MAX_LENGTH)

            binding.confirmBtn.isEnabled = length > 0
            binding.clearBtn.isVisible = binding.inputTxt.hasFocus() && length > 0
        }

        binding.inputTxt.setOnFocusChangeListener { _, hasFocus ->
            updateInputBackground(hasFocus)
            binding.clearBtn.isVisible = hasFocus && !binding.inputTxt.text.isNullOrEmpty()
            binding.countTxt.isVisible = hasFocus
        }

        binding.clearBtn.setOnClickListener {
            binding.inputTxt.text?.clear()
        }
    }

    private fun showError() {
        isError = true
        binding.inputContainer.setBackgroundResource(com.baek.diract.R.drawable.bg_input_error)
        binding.countTxt.isVisible = false
        binding.errorTxt.isVisible = true
    }

    private fun clearError() {
        isError = false
        binding.inputContainer.setBackgroundResource(com.baek.diract.R.drawable.bg_input_focus)
        binding.errorTxt.isVisible = false
        binding.countTxt.isVisible = true
    }

    private fun updateInputBackground(hasFocus: Boolean) {
        if (isError) return
        val bgRes =
            if (hasFocus) com.baek.diract.R.drawable.bg_input_focus else com.baek.diract.R.drawable.bg_input_multi_line_default
        binding.inputContainer.setBackgroundResource(bgRes)
    }

    private fun setupConfirmButton() {
        binding.confirmBtn.isEnabled = false

        binding.confirmBtn.setOnClickListener {
            val content = binding.inputTxt.text?.toString().orEmpty()
            if (content.isBlank()) return@setOnClickListener

            viewModel.submitReport(content)
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.actionContainer) { v, insets ->
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())

            val bottomPadding = maxOf(ime.bottom, navBar.bottom)
            val vertPadding =
                resources.getDimensionPixelSize(com.baek.diract.R.dimen.action_container_vert_padding)
            v.updatePadding(bottom = bottomPadding + vertPadding)

            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            if (!imeVisible) {
                binding.inputTxt.clearFocus()
            }

            insets
        }
    }

    @Suppress("ClickableViewAccessibility")
    private fun setupTouchOutsideToDismissKeyboard() {
        binding.root.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN && binding.inputTxt.hasFocus()) {
                binding.inputTxt.clearFocus()
                val insetsController = WindowInsetsControllerCompat(
                    requireActivity().window, v
                )
                insetsController.hide(WindowInsetsCompat.Type.ime())
            }
            false
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.reportState.collect { state ->
                        val isLoading = state is UiState.Loading || state is UiState.Success
                        binding.confirmBtn.visibility =
                            if (isLoading) View.GONE else View.VISIBLE
                        binding.loadingView.visibility =
                            if (isLoading) View.VISIBLE else View.GONE
                        binding.blockingView.visibility =
                            if (isLoading) View.VISIBLE else View.GONE

                        when (state) {
                            is UiState.Success -> {
                                dismiss()
                            }

                            is UiState.Error -> {
                                // 초기 상태로 복원
                                binding.confirmBtn.isEnabled =
                                    !binding.inputTxt.text.isNullOrEmpty()
                            }

                            else -> {}
                        }
                    }
                }
                launch {
                    viewModel.toastEvent.collect { event ->
                        if (event.isErr) {
                            CustomToast.showNegative(requireContext(), event.txtRes)
                        } else {
                            CustomToast.showPositive(requireContext(), event.txtRes)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ReportDialogFragment"
        private const val MAX_LENGTH = 100

        fun newInstance(
            contentType: ReportType,
            targetId: String,
            reportedId: String
        ): ReportDialogFragment {
            return ReportDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ReportViewModel.ARG_CONTENT_TYPE, contentType.type)
                    putString(ReportViewModel.ARG_TARGET_ID, targetId)
                    putString(ReportViewModel.ARG_REPORTED_ID, reportedId)
                }
            }
        }
    }
}
