package com.baek.diract.presentation.login

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.baek.diract.BuildConfig
import com.baek.diract.databinding.FragmentLoginBinding
import com.baek.diract.presentation.common.CustomToast
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.UUID

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by activityViewModels()

    private val credentialManager by lazy { CredentialManager.create(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupLoginButton()
        observeAuthState()
    }

    private fun setupLoginButton() {
        binding.loginBtn.setOnClickListener {
            requestGoogleLogin()
        }
    }

    private fun requestGoogleLogin() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.WEB_CLIENT_ID)
            .setNonce(UUID.randomUUID().toString())
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        Log.d(TAG, "Google 로그인 요청 시작")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(requireActivity(), request)
                Log.d(TAG, "Credential 수신 성공")
                handleSignInResult(result)
            } catch (e: GetCredentialCancellationException) {
                Log.d(TAG, "사용자가 로그인을 취소함")
            } catch (e: Exception) {
                Log.e(TAG, "Google 로그인 실패", e)
                CustomToast.showNegative(
                    requireContext(),
                    e.message ?: "구글 로그인에 실패했습니다.",
                    Toast.LENGTH_LONG
                )
            }
        }
    }

    private fun handleSignInResult(result: GetCredentialResponse) {
        when (val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data)
                    Log.d(TAG, "Google ID Token 수신: ${googleIdToken.idToken.take(20)}...")
                    googleIdToken.displayName?.let { viewModel.setGoogleDisplayName(it) }
                    viewModel.loginWithGoogle(googleIdToken.idToken)
                } else {
                    Log.w(TAG, "예상치 못한 Credential 타입: ${credential.type}")
                }
            }

            else -> {
                Log.w(TAG, "예상치 못한 Credential: ${credential.javaClass.simpleName}")
            }
        }
    }

    private fun observeAuthState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.authState.collect { state ->
                    when (state) {
                        is AuthState.Loading -> {
                            binding.loginBtn.isEnabled = false
                        }
                        is AuthState.Error -> {
                            binding.loginBtn.isEnabled = true
                            CustomToast.showNegative(
                                requireContext(),
                                state.message,
                                Toast.LENGTH_LONG
                            )
                        }
                        else -> {
                            binding.loginBtn.isEnabled = true
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "LoginFragment"
    }
}
