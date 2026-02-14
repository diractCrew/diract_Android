package com.baek.diract.presentation.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.baek.diract.R
import com.baek.diract.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    //바텀 네비게이션바 필요한 뷰
    private val bottomDestinations = setOf(
        R.id.homeFragment,
        R.id.inboxFragment,
        R.id.myPageFragment
    )

    //상태바까지 확장할 뷰
    private val edgeToEdgeDestinations: Set<Int> = setOf(
        R.id.videoListFragment
    )

    //상태바 확장 상태
    private var isEdgeToEdge: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //인셋 리스너 설정
        ViewCompat.setOnApplyWindowInsetsListener(binding.navHostFragment) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, if (isEdgeToEdge) 0 else bars.top, 0, 0)
            insets
        }

        //바텀 네비게이션 설정
        val navHost =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHost.navController

        binding.bottomNav.setupWithNavController(navController)


        //화면 전환에 따른 상태 변경(바텀 네비게이션바, 상태바 뷰 확장)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            //바텀 네비게이션 여부 결정
            binding.bottomNav.visibility =
                if (destination.id in bottomDestinations) View.VISIBLE else View.GONE

            //상태바 확장 상태 변경
            val newIsEdgeToEdge = destination.id in edgeToEdgeDestinations
            //상태 바뀌면 인셋 적용
            if (isEdgeToEdge != newIsEdgeToEdge) {
                isEdgeToEdge = newIsEdgeToEdge
                ViewCompat.requestApplyInsets(binding.navHostFragment)
            }
        }

        //초기 인셋 적용
        ViewCompat.requestApplyInsets(binding.navHostFragment)

        requestNotificationPermission()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val isGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (isGranted) return

        // 이미 한 번 요청했으면 다시 요청하지 않음
        val prefs = getPreferences(MODE_PRIVATE)
        val alreadyRequested = prefs.getBoolean(KEY_NOTIFICATION_REQUESTED, false)
        if (alreadyRequested) return

        prefs.edit().putBoolean(KEY_NOTIFICATION_REQUESTED, true).apply()
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    companion object {
        private const val KEY_NOTIFICATION_REQUESTED = "notification_permission_requested"
    }
}
