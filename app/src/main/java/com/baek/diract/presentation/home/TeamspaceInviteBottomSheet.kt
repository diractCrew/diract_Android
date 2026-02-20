package com.baek.diract.presentation.home

import com.baek.diract.R
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 스타일을 강제로 지정하여 투명화 현상 방지
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
        // 다이얼로그의 창 크기를 화면에 꽉 차게 강제 설정
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            // ✅ 레이아웃 높이를 MATCH_PARENT로 강제
            it.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = it.height // 현재 높이를 피크 높이로 설정
            behavior.skipCollapsed = true
        }
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
