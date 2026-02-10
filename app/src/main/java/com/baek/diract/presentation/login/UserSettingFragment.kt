package com.baek.diract.presentation.login

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.baek.diract.databinding.FragmentUserSettingBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserSettingFragment : Fragment() {

    private var _binding: FragmentUserSettingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeGoogleDisplayName()
        setupNameValidation()
        setupConfirmButton()
        observeProfileSaving()
    }

    private fun observeGoogleDisplayName() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.googleDisplayName.collect { name ->
                    if (name.isNotBlank() && binding.nameEditTxt.text.isNullOrEmpty()) {
                        binding.nameEditTxt.setText(name)
                    }
                }
            }
        }
    }

    // 이름 입력 칸이 비어있으면 확인 버튼 비활성화
    private fun setupNameValidation() {
        updateConfirmButtonState()
        binding.nameEditTxt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateConfirmButtonState()
            }
        })
    }

    private fun updateConfirmButtonState() {
        binding.confirmBtn.isEnabled = !binding.nameEditTxt.text.isNullOrBlank()
    }

    private fun setupConfirmButton() {
        binding.confirmBtn.setOnClickListener {
            val name = binding.nameEditTxt.text.toString().trim()
            viewModel.updateMyName(name)
        }
    }

    // 로딩 상태에 따라 loadingView / confirmBtn 전환
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
