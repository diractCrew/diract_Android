package com.baek.diract.presentation.home

import com.baek.diract.R
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.baek.diract.databinding.FragmentTeamspaceInviteBinding
import com.baek.diract.presentation.common.CustomToast
import com.baek.diract.presentation.common.UiState
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class TeamspaceInviteBottomSheet : BottomSheetDialogFragment() {
    companion object {
        private const val ARG_TEAMSPACE_ID = "teamspaceId"

        fun newInstance(teamspaceId: String) = TeamspaceInviteBottomSheet().apply {
            arguments = Bundle().apply { putString(ARG_TEAMSPACE_ID, teamspaceId) }
        }
    }

    private var _binding: FragmentTeamspaceInviteBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TeamspaceInviteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.Theme_Design_Light_BottomSheetDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        android.util.Log.e("InviteFlow", "=== 초대 시트 onCreateView 호출됨! ===")
        _binding = FragmentTeamspaceInviteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showDefault()

        binding.closeBtn.setOnClickListener { dismiss() }
        binding.btnInviteLater.setOnClickListener { dismiss() }

        binding.inviteBtn.setOnClickListener {
            viewModel.createInviteLink()
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.inviteState.collect { state ->
                    when (state) {
                        is UiState.Loading -> showLoading()
                        is UiState.Success -> {
                            copyToClipboard(state.data)
                            showComplete()
                        }
                        is UiState.Error -> {
                            showDefault()
                            CustomToast.showNegative(
                                requireContext(),
                                R.string.teamspace_invite_link_failed,
                                Toast.LENGTH_SHORT
                            )
                        }
                        is UiState.None -> Unit
                    }
                }
            }
        }
    }

    private fun copyToClipboard(url: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("invite_link", url))
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            it.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = it.height
            behavior.skipCollapsed = true
        }
    }

    private fun showDefault() {
        binding.inviteBtn.visibility = View.VISIBLE
        binding.loadingView.visibility = View.GONE
        binding.completeView.visibility = View.GONE
        binding.blockingView.visibility = View.GONE
    }

    private fun showLoading() {
        binding.inviteBtn.visibility = View.GONE
        binding.loadingView.visibility = View.VISIBLE
        binding.completeView.visibility = View.GONE
        binding.blockingView.visibility = View.VISIBLE
    }

    private fun showComplete() {
        binding.inviteBtn.visibility = View.GONE
        binding.loadingView.visibility = View.GONE
        binding.completeView.visibility = View.VISIBLE
        binding.blockingView.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}