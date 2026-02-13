package com.baek.diract.presentation.mypage

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.baek.diract.R
import com.baek.diract.databinding.FragmentAccountSettingBinding
import com.baek.diract.presentation.common.CustomToast
import com.baek.diract.presentation.common.LoadingOverlay
import com.baek.diract.presentation.common.dialog.BasicDialog
import com.baek.diract.presentation.login.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.getValue

@AndroidEntryPoint
class AccountSettingFragment : Fragment() {

    private var _binding: FragmentAccountSettingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MyPageViewModel by hiltNavGraphViewModels(R.id.mypage_nav_graph)
    private val loadingOverlay by lazy { LoadingOverlay(this) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
        setupOnClickListener()
        setupWindowInsets()
    }

    private fun setupOnClickListener() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.logoutBtn.setOnClickListener {
            BasicDialog.destructive(
                context = requireContext(),
                title = getString(R.string.logout),
                message = getString(R.string.dialog_logout_content),
                positiveText = getString(R.string.logout),
                onPositive = { viewModel.logout() }
            ).show()
        }

        binding.confirmBtn.setOnClickListener {
            BasicDialog.destructive(
                context = requireContext(),
                title = getString(R.string.dialog_delete_account_title),
                message = getString(R.string.dialog_delete_account_content),
                positiveText = getString(R.string.dialog_delete_account_btn),
                onPositive = { viewModel.deleteAccount() }
            ).show()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.userInfo.collect { user ->
                        binding.emailTxt.text = user?.email ?: getString(R.string.unknown)
                    }
                }
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        loadingOverlay.setVisible(isLoading)
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
                    viewModel.navigateToLogin.collect {
                        val intent = Intent(requireContext(), LoginActivity::class.java)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }
                }
            }
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.actionContainer) { v, insets ->
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            val vertPadding = resources.getDimensionPixelSize(R.dimen.action_container_vert_padding)
            v.updatePadding(bottom = navBar.bottom + vertPadding)
            insets
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
