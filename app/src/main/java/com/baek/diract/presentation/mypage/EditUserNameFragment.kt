package com.baek.diract.presentation.mypage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.activity.OnBackPressedCallback
import androidx.navigation.fragment.findNavController
import com.baek.diract.R
import com.baek.diract.databinding.FragmentEditUserNameBinding
import com.baek.diract.presentation.common.CustomToast
import com.baek.diract.presentation.common.MaxLengthInputFilter
import com.baek.diract.presentation.common.dialog.BasicDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.getValue

@AndroidEntryPoint
class EditUserNameFragment : Fragment() {

    private var _binding: FragmentEditUserNameBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MyPageViewModel by hiltNavGraphViewModels(R.id.mypage_nav_graph)

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
        setupBackPressHandler()
        setupInput()
        setupConfirmButton()
        observeViewModel()
        setupWindowInsets()
        setupTouchOutsideToDismissKeyboard()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            handleBackPress()
        }
    }

    private fun setupBackPressHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleBackPress()
                }
            }
        )
    }

    private fun handleBackPress() {
        val hasInput = binding.nameEditTxt.text?.isNotEmpty() == true
        if (hasInput) {
            BasicDialog.destructive(
                context = requireContext(),
                title = getString(R.string.dialog_discard_title),
                message = getString(R.string.dialog_discard_message),
                positiveText = getString(R.string.dialog_exit),
                onPositive = { findNavController().navigateUp() }
            ).show()
        } else {
            findNavController().navigateUp()
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
                    getString(R.string.input_dialog_error_max_length, MAX_LENGTH)
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

            viewModel.updateMyName(name)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.userInfo.collect { user ->
                        user?.let { binding.nameEditTxt.setText(it.name) }
                    }
                }
                launch {
                    viewModel.isUpdatingName.collect { isUpdating ->
                        binding.confirmBtn.visibility = if (isUpdating) View.GONE else View.VISIBLE
                        binding.loadingView.visibility = if (isUpdating) View.VISIBLE else View.GONE
                        binding.blockingView.visibility =
                            if (isUpdating) View.VISIBLE else View.GONE
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
                launch {
                    viewModel.navigateBack.collect {
                        findNavController().navigateUp()
                    }
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
