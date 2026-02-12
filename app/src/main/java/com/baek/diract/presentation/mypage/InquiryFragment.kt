package com.baek.diract.presentation.mypage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.baek.diract.R
import com.baek.diract.databinding.FragmentInquiryBinding
import com.baek.diract.presentation.common.MaxLengthInputFilter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InquiryFragment : Fragment() {

    private var _binding: FragmentInquiryBinding? = null
    private val binding get() = _binding!!

    private var isError = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInquiryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupInput()
        setupConfirmButton()
        setupWindowInsets()
        setupTouchOutsideToDismissKeyboard()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupInput() {
        binding.inputTxt.filters = arrayOf(
            MaxLengthInputFilter(MAX_LENGTH) { showError() }
        )

        binding.inputTxt.doAfterTextChanged { text ->
            val length = text?.length ?: 0

            // 에러 해제
            if (isError && length < MAX_LENGTH) {
                clearError()
            }

            // 카운터 업데이트
            binding.countTxt.text = getString(R.string.input_dialog_counter, length, MAX_LENGTH)

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
        binding.inputContainer.setBackgroundResource(R.drawable.bg_input_error)
        binding.countTxt.isVisible = false
        binding.errorTxt.isVisible = true
    }

    private fun clearError() {
        isError = false
        binding.inputContainer.setBackgroundResource(R.drawable.bg_input_focus)
        binding.errorTxt.isVisible = false
        binding.countTxt.isVisible = true
    }

    private fun updateInputBackground(hasFocus: Boolean) {
        if (isError) return
        val bgRes = if (hasFocus) R.drawable.bg_input_focus else R.drawable.bg_input_multi_line_default
        binding.inputContainer.setBackgroundResource(bgRes)
    }

    private fun setupConfirmButton() {
        binding.confirmBtn.isEnabled = false

        binding.confirmBtn.setOnClickListener {
            val content = binding.inputTxt.text?.toString().orEmpty()
            if (content.isBlank()) return@setOnClickListener

            // TODO: 문의 접수 API 호출
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.actionContainer) { v, insets ->
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())

            val bottomPadding = maxOf(ime.bottom, navBar.bottom)
            val vertPadding = resources.getDimensionPixelSize(R.dimen.action_container_vert_padding)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val MAX_LENGTH = 100
    }
}