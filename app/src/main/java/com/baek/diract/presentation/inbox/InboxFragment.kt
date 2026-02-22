package com.baek.diract.presentation.inbox

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.baek.diract.databinding.FragmentInboxBinding
import com.baek.diract.domain.model.Notification
import com.baek.diract.presentation.common.UiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class InboxFragment : Fragment() {
    private var _binding: FragmentInboxBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InboxViewModel by viewModels()
    private lateinit var notificationAdapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInboxBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAdapter()
        initView()
        observeViewModel()
    }

    private fun initAdapter() {
        notificationAdapter = NotificationAdapter(onClick = ::onNotificationClick)
        binding.rvNotification.adapter = notificationAdapter
    }

    private fun initView() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadNotifications()
        }
        binding.reloadBtn.setOnClickListener {
            viewModel.loadNotifications()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is UiState.Loading -> {
                            // SwipeRefresh 중이면 기존 목록 유지, 초기 로드면 로딩 뷰 표시
                            if (notificationAdapter.itemCount == 0) showLoading()
                        }
                        is UiState.Success -> {
                            binding.swipeRefreshLayout.isRefreshing = false
                            showList(state.data)
                        }
                        is UiState.Error -> {
                            binding.swipeRefreshLayout.isRefreshing = false
                            showError()
                        }
                        is UiState.None -> Unit
                    }
                }
            }
        }
    }

    private fun showLoading() {
        binding.loadingView.visibility = View.VISIBLE
        binding.rvNotification.visibility = View.GONE
        binding.emptyView.visibility = View.GONE
        binding.errorView.visibility = View.GONE
    }

    private fun showList(notifications: List<Notification>) {
        notificationAdapter.submitList(notifications)
        binding.loadingView.visibility = View.GONE
        binding.errorView.visibility = View.GONE
        val isEmpty = notifications.isEmpty()
        binding.rvNotification.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun showError() {
        binding.loadingView.visibility = View.GONE
        binding.rvNotification.visibility = View.GONE
        binding.emptyView.visibility = View.GONE
        binding.errorView.visibility = View.VISIBLE
    }

    private fun onNotificationClick(notification: Notification) {
        val videoId = notification.videoId ?: return
        viewModel.markAsRead(notification.notificationId)
        val action = InboxFragmentDirections.actionInboxFragmentToVideoPlayerFragment(
            videoId = videoId,
            videoTitle = notification.videoTitle,
            teamspaceId = notification.teamspaceId
        )
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}