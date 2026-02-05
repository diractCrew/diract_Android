package com.baek.diract.presentation.common.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import androidx.core.view.isVisible
import com.baek.diract.R
import com.baek.diract.databinding.FragmentInputDialogBinding
import com.baek.diract.presentation.common.MaxLengthInputFilter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * 입력 다이얼로그 컴포넌트
 *
 * 사용 예시:
 * ```
 * val dialog = InputDialogFragment.newInstance(
 *     title = "새 프로젝트 만들기",
 *     description = "프로젝트 이름을 입력하세요.",
 *     hint = "(예시) 대동제",
 *     buttonText = "새 프로젝트 만들기",
 *     maxLength = 20
 * ).apply {
 *     onConfirm = { inputText ->
 *
 *         // 확인 버튼 클릭 시 처리
 *     }
 * }.show(..)
 *
 * // 다이얼로그 표시
 *
 * //로딩중일떼
 * dialog.showLoading()
 *
 * // 완료되었을 때
 * dialog.showComplete()
 *
 * //다시 기본 화면 보여줄때
 * dialog.showDefault()
 * ```
 */
class InputDialogFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentInputDialogBinding? = null
    private val binding get() = _binding!!
    var onConfirm: ((String) -> Unit)? = null

    private val maxLength: Int
        get() = arguments?.getInt(ARG_MAX_LENGTH, DEFAULT_MAX_LENGTH) ?: DEFAULT_MAX_LENGTH

    private var isError = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                Log.d("InputDialogFragment", "backPress됨")
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
        _binding = FragmentInputDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFullScreen()
        setupViews()
        setupTextWatcher()
    }


    private fun setupFullScreen() {
        (dialog as? BottomSheetDialog)?.let { bottomSheetDialog ->
            bottomSheetDialog.setOnShowListener {
                val bottomSheet = bottomSheetDialog.findViewById<FrameLayout>(
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
    }


    private fun setupViews() {
        val title = arguments?.getString(ARG_TITLE) ?: ""
        val description = arguments?.getString(ARG_DESCRIPTION) ?: ""
        val initialText = arguments?.getString(ARG_INITIAL_TEXT) ?: ""
        val hint = arguments?.getString(ARG_HINT)
        val buttonText = arguments?.getString(ARG_BUTTON_TEXT) ?: getString(R.string.dialog_confirm)
        val completeText =
            arguments?.getString(ARG_COMPLETE_TEXT) ?: getString(R.string.dialog_default_complete)

        binding.titleTxt.text = title
        binding.descriptionTxt.text = description
        binding.confirmBtn.text = buttonText
        binding.completeView.text = completeText

        hint?.let { binding.inputTxt.hint = it }
        binding.inputTxt.filters = arrayOf(MaxLengthInputFilter(maxLength) { showMaxLengthError() })
        binding.inputTxt.setText(initialText)

        // 입력 필드에 포커스 및 커서를 끝으로 이동
        binding.inputTxt.requestFocus()
        binding.inputTxt.setSelection(binding.inputTxt.text?.length ?: 0)

        // 초기 상태 설정
        showDefault()
        updateButtonState(initialText)
        updateClearButtonVisibility(initialText)
        updateCounter(initialText.length)

        binding.closeBtn.setOnClickListener {
            handleBackPress()
        }

        binding.clearBtn.setOnClickListener {
            binding.inputTxt.text?.clear()
        }

        binding.confirmBtn.setOnClickListener {
            val imm =
                view?.context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view?.windowToken, 0)

            val inputText = binding.inputTxt.text?.toString()?.trim() ?: ""
            if (inputText.isNotEmpty() && inputText.length <= maxLength) {
                onConfirm?.invoke(inputText)
            }
        }


        // EditText 포커스 변경에 따른 배경 처리
        binding.inputTxt.setOnFocusChangeListener { _, hasFocus ->
            updateInputBackground(hasFocus)
        }

        // 키보드가 내려가면 포커스 해제
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            if (!imeVisible) {
                binding.inputTxt.clearFocus()
            }
            insets
        }
    }

    fun showDefault() {
        binding.confirmBtn.visibility = View.VISIBLE
        binding.loadingView.visibility = View.GONE
        binding.completeView.visibility = View.GONE
        binding.blockingView.visibility = View.GONE
    }

    /** 로딩 상태로 변경 */
    fun showLoading() {
        binding.confirmBtn.visibility = View.GONE
        binding.loadingView.visibility = View.VISIBLE
        binding.completeView.visibility = View.GONE
        binding.blockingView.visibility = View.VISIBLE
    }

    /** 완료 상태로 변경 */
    fun showComplete() {
        binding.confirmBtn.visibility = View.GONE
        binding.loadingView.visibility = View.GONE
        binding.completeView.visibility = View.VISIBLE
        binding.blockingView.visibility = View.VISIBLE
    }

    private fun setupTextWatcher() {
        binding.inputTxt.addTextChangedListener(object : TextWatcher {
            private var previousLength = 0

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                previousLength = s?.length ?: 0
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val currentLength = s?.length ?: 0
                // 텍스트 삭제 시 에러 해제
                if (currentLength < previousLength) {
                    clearError()
                }
            }

            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString() ?: ""
                updateButtonState(text)
                updateClearButtonVisibility(text)
                updateCounter(text.length)
            }
        })
    }

    private fun showMaxLengthError() {
        isError = true
        binding.counterTxt.isVisible = true
        binding.counterTxt.text = getString(R.string.input_dialog_error_max_length, maxLength)
        binding.counterTxt.setTextColor(requireContext().getColor(R.color.accent_red_normal))
        updateInputBackground(binding.inputTxt.hasFocus())
    }

    private fun updateButtonState(text: String) {
        binding.confirmBtn.isEnabled = text.isNotEmpty() && text.length <= maxLength
    }

    private fun updateClearButtonVisibility(text: String) {
        binding.clearBtn.isVisible = text.isNotEmpty()
    }

    private fun updateCounter(length: Int) {
        if (length > 0) {
            binding.counterTxt.isVisible = true
            // 에러 상태가 아닐 때만 카운터 표시 (에러 상태면 에러 메시지 유지)
            if (!isError) {
                binding.counterTxt.text =
                    getString(R.string.input_dialog_counter, length, maxLength)
                binding.counterTxt.setTextColor(requireContext().getColor(R.color.secondary_normal))
            }
            updateInputBackground(binding.inputTxt.hasFocus())
        } else {
            isError = false
            binding.counterTxt.isVisible = false
            updateInputBackground(binding.inputTxt.hasFocus())
        }
    }

    private fun clearError() {
        isError = false
    }

    private fun updateInputBackground(hasFocus: Boolean) {
        val backgroundRes = when {
            isError -> R.drawable.bg_input_error
            hasFocus -> R.drawable.bg_input_focus
            else -> R.drawable.bg_input_default
        }
        binding.inputContainer.setBackgroundResource(backgroundRes)
    }

    override fun onStart() {
        super.onStart()
    }


    private fun handleBackPress() {
        BasicDialog.destructive(
            context = requireContext(),
            title = getString(R.string.dialog_discard_title),
            message = getString(R.string.dialog_discard_message),
            positiveText = getString(R.string.dialog_exit),
            onPositive = {
                dismiss()
            }
        ).show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "InputDialogFragment"
        private const val DEFAULT_MAX_LENGTH = 20

        private const val ARG_TITLE = "arg_title"
        private const val ARG_DESCRIPTION = "arg_description"
        private const val ARG_INITIAL_TEXT = "arg_initial_text"
        private const val ARG_HINT = "arg_hint"
        private const val ARG_BUTTON_TEXT = "arg_button_text"

        private const val ARG_COMPLETE_TEXT = "arg_complete_text"
        private const val ARG_MAX_LENGTH = "arg_max_length"

        fun newInstance(
            title: String,
            description: String = "",
            hint: String? = null,
            initialText: String = "",
            buttonText: String? = null,
            completeText: String? = null,
            maxLength: Int = DEFAULT_MAX_LENGTH
        ): InputDialogFragment {
            return InputDialogFragment().apply {
                arguments = bundleOf(
                    ARG_TITLE to title,
                    ARG_DESCRIPTION to description,
                    ARG_INITIAL_TEXT to initialText,
                    ARG_HINT to hint,
                    ARG_BUTTON_TEXT to buttonText,
                    ARG_COMPLETE_TEXT to completeText,
                    ARG_MAX_LENGTH to maxLength
                )
            }
        }
    }
}
