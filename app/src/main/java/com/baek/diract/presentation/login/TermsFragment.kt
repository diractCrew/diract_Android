package com.baek.diract.presentation.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.baek.diract.R
import com.baek.diract.databinding.FragmentTermsBinding
import com.baek.diract.presentation.common.WebViewDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TermsFragment : Fragment() {

    companion object {
        private const val PRIVACY_POLICY_URL =
            "https://mammoth-eyelash-f4f.notion.site/29610840462c8014ba1be32d01ef3edb"
        private const val TERMS_OF_SERVICE_URL =
            "https://mammoth-eyelash-f4f.notion.site/29610840462c8038a85bf08362518b03"
    }

    private var _binding: FragmentTermsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTermsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeTermsState()
    }

    private fun setupListeners() {
        binding.agreeAllBtn.setOnClickListener { viewModel.toggleAllAgreed() }
        binding.agreePrivacyBtn.setOnClickListener { viewModel.togglePrivacyAgreed() }
        binding.agreeServiceBtn.setOnClickListener { viewModel.toggleServiceAgreed() }
        binding.agreeAgeBtn.setOnClickListener { viewModel.toggleAgeAgreed() }

        binding.privacyBtn.setOnClickListener {
            WebViewDialogFragment.newInstance(
                title = getString(R.string.agree_privacy_policy),
                url = PRIVACY_POLICY_URL
            ).show(childFragmentManager, WebViewDialogFragment.TAG)
        }

        binding.serviceBtn.setOnClickListener {
            WebViewDialogFragment.newInstance(
                title = getString(R.string.agree_terms_of_service),
                url = TERMS_OF_SERVICE_URL
            ).show(childFragmentManager, WebViewDialogFragment.TAG)
        }

        binding.confirmBtn.setOnClickListener {
            viewModel.savePendingTokens()
            val action = TermsFragmentDirections.actionTermsFragmentToUserSettingFragment()
            findNavController().navigate(action)
        }
    }

    private fun observeTermsState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isPrivacyAgreed.collect {
                        binding.agreePrivacyBtn.isSelected = it
                        updateAllAgreedState()
                    }
                }
                launch {
                    viewModel.isServiceAgreed.collect {
                        binding.agreeServiceBtn.isSelected = it
                        updateAllAgreedState()
                    }
                }
                launch {
                    viewModel.isAgeAgreed.collect {
                        binding.agreeAgeBtn.isSelected = it
                        updateAllAgreedState()
                    }
                }
            }
        }
    }

    private fun updateAllAgreedState() {
        val allAgreed = viewModel.isPrivacyAgreed.value
                && viewModel.isServiceAgreed.value
                && viewModel.isAgeAgreed.value
        binding.agreeAllBtn.isSelected = allAgreed
        binding.confirmBtn.isEnabled = allAgreed
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
