package com.baek.diract.presentation.mypage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.baek.diract.R
import com.baek.diract.databinding.FragmentMyPageBinding
import com.baek.diract.presentation.common.WebViewDialogFragment
import com.baek.diract.presentation.login.TermsFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MyPageFragment : Fragment() {
    private var _binding: FragmentMyPageBinding? = null
    private val binding get() = _binding!!


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

    /*
        TODO: HomeFragment에서 navigation 설정 필요
        1. nav_graph에서 수정
        2. home fragment에서
            val action = HomeFragmentDirections.actionHomeFragmentToVideoListFragment(tracksId,tracksTitle)
            findNavController().navigate(action)
     */
    private fun navigationTemporaryEx() {
        val tracksId = "AndroidTestTracks1"
        val tracksTitle = "AndTracks"
        binding.toVideoBtn.setOnClickListener {
            val action = MyPageFragmentDirections.actionMyPageFragmentToVideoNavGraph(
                tracksId,
                tracksTitle
            )
            findNavController().navigate(action)
        }
    }

    /*
    TEST_EMAIL = "android1234@android.com"
       TEST_PASSWORD = "android1234"
     */

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
