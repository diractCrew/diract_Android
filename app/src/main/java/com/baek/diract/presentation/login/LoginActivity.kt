package com.baek.diract.presentation.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import com.baek.diract.R
import com.baek.diract.databinding.ActivityLoginBinding
import com.baek.diract.presentation.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        observeAuthState()
    }

    private fun observeAuthState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.authState.collect { state ->
                    val showLoading = state is AuthState.Loading || state is AuthState.LoggedIn
                    binding.loadingView.visibility = if (showLoading) View.VISIBLE else View.GONE
                    binding.navHostFragment.visibility = if (showLoading) View.GONE else View.VISIBLE

                    when (state) {
                        is AuthState.LoggedIn -> {
                            Log.d(TAG, "로그인 완료 → MainActivity 이동")
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish()
                        }

                        is AuthState.NeedsSignUp -> {
                            Log.d(TAG, "신규 유저 → TermsFragment 이동")
                            navigateToTerms()
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    private fun navigateToTerms() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        // 이미 termsFragment에 있으면 중복 이동 방지
        if (navController.currentDestination?.id != R.id.termsFragment) {
            navController.navigate(R.id.action_loginFragment_to_termsFragment)
        }
    }

    companion object {
        private const val TAG = "LoginActivity"
    }
}
