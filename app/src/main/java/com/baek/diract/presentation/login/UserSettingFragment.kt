package com.baek.diract.presentation.login

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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.baek.diract.R
import com.baek.diract.databinding.FragmentUserSettingBinding
import com.baek.diract.presentation.common.CustomToast
import com.baek.diract.presentation.common.MaxLengthInputFilter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserSettingFragment : Fragment() {

    private var _binding: FragmentUserSettingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by activityViewModels()

    private var isError = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeUserInfo()
        setupInput()
        setupConfirmButton()
        observeProfileSaving()
        setupWindowInsets()
        setupTouchOutsideToDismissKeyboard()
    }

    private fun observeUserInfo() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentUserInfo.collect { user ->
                    if (user != null && user.name.isNotBlank() && binding.nameEditTxt.text.isNullOrEmpty()) {
                        binding.nameEditTxt.setText(user.name)
                    }
                }
            }
        }
    }

    private fun setupInput() {
        binding.confirmBtn.isEnabled = !binding.nameEditTxt.text.isNullOrEmpty()

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
        binding.confirmBtn.setOnClickListener {
            val name = binding.nameEditTxt.text.toString().trim()
            viewModel.updateMyName(name)
        }
    }

    private fun observeProfileSaving() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isProfileSaving.collect { isSaving ->
                    binding.loadingView.visibility = if (isSaving) View.VISIBLE else View.GONE
                    binding.confirmBtn.visibility = if (isSaving) View.GONE else View.VISIBLE
                }
            }
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
