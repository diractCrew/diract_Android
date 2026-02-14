package com.baek.diract.presentation.mypage

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.baek.diract.R
import com.baek.diract.databinding.FragmentMyPageBinding
import com.baek.diract.presentation.common.WebViewDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MyPageFragment : Fragment() {
    private var _binding: FragmentMyPageBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MyPageViewModel by hiltNavGraphViewModels(R.id.mypage_nav_graph)


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupOnClickListener()
        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userInfo.collect { user ->
                    binding.emailTxt.text = user?.email ?: getString(R.string.unknown)
                    binding.nameBtn.text = user?.name ?: getString(R.string.unknown)
                }
            }
        }
    }
    private fun setupOnClickListener() {

        binding.nameBtn.setOnClickListener {
            val action = MyPageFragmentDirections.actionMyPageFragmentToEditUserNameFragment()
            findNavController().navigate(action)
        }

        binding.privacyPolicyBtn.setOnClickListener {
            WebViewDialogFragment.newInstance(
                title = getString(R.string.agree_privacy_policy),
                url = PRIVACY_POLICY_URL
            ).show(childFragmentManager, WebViewDialogFragment.TAG)
        }
        binding.termsOfServiceBtn.setOnClickListener {
            WebViewDialogFragment.newInstance(
                title = getString(R.string.agree_terms_of_service),
                url = TERMS_OF_SERVICE_URL
            ).show(childFragmentManager, WebViewDialogFragment.TAG)
        }
        binding.notificationBtn.setOnClickListener {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
            }
            startActivity(intent)
        }

        binding.accountSettingBtn.setOnClickListener {
            val action = MyPageFragmentDirections.actionMyPageFragmentToAccountSettingFragment()
            findNavController().navigate(action)
        }

        binding.inquiryBtn.setOnClickListener {
            val action = MyPageFragmentDirections.actionMyPageFragmentToInquiryFragment()
            findNavController().navigate(action)
        }

        binding.aboutDiractBtn.setOnClickListener {
            val action = MyPageFragmentDirections.actionMyPageFragmentToAboutDiractFragment()
            findNavController().navigate(action)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val PRIVACY_POLICY_URL =
            "https://mammoth-eyelash-f4f.notion.site/29610840462c8014ba1be32d01ef3edb"
        private const val TERMS_OF_SERVICE_URL =
            "https://mammoth-eyelash-f4f.notion.site/29610840462c8038a85bf08362518b03"
    }
}
