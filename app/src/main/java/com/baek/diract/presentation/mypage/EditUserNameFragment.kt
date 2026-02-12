package com.baek.diract.presentation.mypage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.baek.diract.R
import com.baek.diract.databinding.FragmentEditUserNameBinding
import com.baek.diract.presentation.common.CustomToast
import com.baek.diract.presentation.common.MaxLengthInputFilter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditUserNameFragment : Fragment() {

    private var _binding: FragmentEditUserNameBinding? = null
    private val binding get() = _binding!!

    private var isError = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditUserNameBinding.inflate(inflater, container, false)
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
        binding.nameEditTxt.filters = arrayOf(
            MaxLengthInputFilter(MAX_LENGTH) {
                isError = true
                binding.editUnderLine.setBackgroundColor(
                    requireContext().getColor(R.color.accent_red_strong)
                )
                CustomToast.showNegative(
                    requireContext(),
                    getString(R.string.input_dialog_error_max_length, MAX_LENGTH),
                    Toast.LENGTH_SHORT
                )
            }
        )

        binding.nameEditTxt.doAfterTextChanged { text ->
            val length = text?.length ?: 0

            if (isError && length < MAX_LENGTH) {
                isError = false
                binding.editUnderLine.setBackgroundColor(
                    requireContext().getColor(R.color.secondary_normal)
                )
            }

            binding.confirmBtn.isEnabled = length > 0
        }

        binding.nameEditTxt.setOnFocusChangeListener { _, hasFocus ->
            if (isError) return@setOnFocusChangeListener
            val colorRes = if (hasFocus) R.color.secondary_normal else R.color.label_assistive
            binding.editUnderLine.setBackgroundColor(requireContext().getColor(colorRes))
        }
    }

    private fun setupConfirmButton() {
        binding.confirmBtn.isEnabled = false

        binding.confirmBtn.setOnClickListener {
            val name = binding.nameEditTxt.text?.toString().orEmpty()
            if (name.isBlank()) return@setOnClickListener

            // TODO: 이름 변경 API 호출
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
                binding.nameEditTxt.clearFocus()
            }

            insets
        }
    }

    @Suppress("ClickableViewAccessibility")
    private fun setupTouchOutsideToDismissKeyboard() {
        binding.root.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN && binding.nameEditTxt.hasFocus()) {
                binding.nameEditTxt.clearFocus()
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
        private const val MAX_LENGTH = 10
    }
}