package com.baek.diract.presentation.home


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.baek.diract.databinding.FragmentTeamspaceInviteBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint


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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTeamspaceInviteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 기본 상태
        showDefault()

        binding.closeBtn.setOnClickListener { dismiss() }
        binding.btnInviteLater.setOnClickListener { dismiss() }

        binding.inviteBtn.setOnClickListener {
            // TODO: 초대 동작 (로딩/완료 상태 전환 가능)
            // showLoading()
        }
    }

    override fun onStart() {
        super.onStart()

        // ✅ "풀"로 강제
        val bottomSheet =
            dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?: return

        bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT

        val behavior = BottomSheetBehavior.from(bottomSheet)
        behavior.skipCollapsed = true
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.isDraggable = false // 필요하면 true로
    }

    private fun showDefault() {
        binding.inviteBtn.visibility = View.VISIBLE
        binding.loadingView.visibility = View.GONE
        binding.completeView.visibility = View.GONE
        binding.blockingView.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
