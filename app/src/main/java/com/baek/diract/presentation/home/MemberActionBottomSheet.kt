package com.baek.diract.presentation.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.baek.diract.R
import com.baek.diract.databinding.BottomsheetMemberActionsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar

class MemberActionBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomsheetMemberActionsBinding? = null
    private val binding get() = _binding!!

    // args
    private val teamspaceId: String by lazy { requireArguments().getString(ARG_TEAMSPACE_ID).orEmpty() }
    private val memberId: String by lazy { requireArguments().getString(ARG_MEMBER_ID).orEmpty() }
    private val memberName: String by lazy { requireArguments().getString(ARG_MEMBER_NAME).orEmpty() }

    // callbacks
    private var onKickClick: ((String, String) -> Unit)? = null
    private var onGiveLeaderClick: ((String, String) -> Unit)? = null
    // (teamspaceId, memberId) 형태로 쓰고 싶으면 여기 타입 바꿔도 됨

    private enum class LeaderState { IDLE, LOADING, SUCCESS }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetMemberActionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = with(binding) {
        tvMemberName.text = memberName

        ivClose.setOnClickListener { dismiss() }

        renderLeaderState(LeaderState.IDLE)

        btnGiveLeader.setOnClickListener {
            // 여기서 실제 API 호출은 Fragment/VM 쪽으로 던지는게 깔끔함
            renderLeaderState(LeaderState.LOADING)

            onGiveLeaderClick?.invoke(teamspaceId, memberId)
            // 성공/실패 결과에 따라 아래 함수를 밖에서 다시 호출해주면 됨
        }

        tvKick.setOnClickListener {
            dismiss()
            onKickClick?.invoke(memberId, memberName)
        }
    }

    /** 외부(프래그먼트)에서 호출해서 UI 갱신 */
    fun showLeaderLoading() {
        if (_binding == null) return
        renderLeaderState(LeaderState.LOADING)
    }

    fun showLeaderSuccessAndClose() {
        if (_binding == null) return
        renderLeaderState(LeaderState.SUCCESS)
        // 잠깐 체크 보여주고 닫고 싶으면 postDelayed로 처리
        binding.root.postDelayed({ dismissAllowingStateLoss() }, 600)
    }

    fun showLeaderFail(message: String) {
        if (_binding == null) return
        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
        renderLeaderState(LeaderState.IDLE)
    }

    private fun renderLeaderState(state: LeaderState) = with(binding) {
        val loading = state == LeaderState.LOADING
        val success = state == LeaderState.SUCCESS

        pbLoading.isVisible = loading
        ivSuccess.isVisible = success

        btnGiveLeader.isEnabled = !loading && !success
        tvKick.isEnabled = !loading && !success
        tvKick.alpha = if (!loading && !success) 1f else 0.35f
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun getTheme(): Int = R.style.ThemeOverlay_Diract_BottomSheetDialog

    companion object {
        private const val ARG_TEAMSPACE_ID = "arg_teamspace_id"
        private const val ARG_MEMBER_ID = "arg_member_id"
        private const val ARG_MEMBER_NAME = "arg_member_name"

        fun newInstance(
            teamspaceId: String,
            memberId: String,
            memberName: String,
            onGiveLeaderClick: (String, String) -> Unit,
            onKickClick: (String, String) -> Unit
        ): MemberActionBottomSheet {
            return MemberActionBottomSheet().apply {
                arguments = bundleOf(
                    ARG_TEAMSPACE_ID to teamspaceId,
                    ARG_MEMBER_ID to memberId,
                    ARG_MEMBER_NAME to memberName
                )
                this.onGiveLeaderClick = onGiveLeaderClick
                this.onKickClick = onKickClick
            }
        }
    }
}
