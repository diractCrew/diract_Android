package com.baek.diract.presentation.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
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

    // callbacks (프래그먼트/VM에서 API 호출)
    private var onKickClick: ((String, String) -> Unit)? = null
    private var onGiveLeaderClick: ((String, String) -> Unit)? = null

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
            // 1) 로딩 UI
            renderLeaderState(LeaderState.LOADING)

            // 2) 실제 API 호출은 Fragment/VM에서 처리
            onGiveLeaderClick?.invoke(teamspaceId, memberId)
        }

        tvKick.setOnClickListener {
            dismiss()
            onKickClick?.invoke(memberId, memberName)
        }
    }

    /** Fragment/VM에서 "요청 시작" 타이밍에 호출해도 되고, 그냥 버튼에서만 처리해도 됨 */
    fun showLeaderLoading() {
        if (_binding == null) return
        renderLeaderState(LeaderState.LOADING)
    }

    /**
     * ✅ 핵심: 성공 UI 잠깐 보여주고 + FragmentResult 쏘고 + 자동 dismiss
     * Fragment/VM에서 API 성공했을 때 이 함수만 호출하면 끝.
     */
    fun showLeaderSuccessAndClose() {
        if (_binding == null) return

        renderLeaderState(LeaderState.SUCCESS)

        // ✅ FragmentResult 전송 (부모 프래그먼트에서 리스너로 받음)
        parentFragmentManager.setFragmentResult(
            REQUEST_KEY_TRANSFER_OWNER,
            bundleOf(
                RESULT_TEAMSPACE_ID to teamspaceId,
                RESULT_NEW_OWNER_ID to memberId,
                RESULT_NEW_OWNER_NAME to memberName
            )
        )

        // ✅ 체크 잠깐 보여주고 내려가기
        binding.root.postDelayed({
            if (isAdded) dismissAllowingStateLoss()
        }, 600)
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

        // ✅ FragmentResult 키들
        const val REQUEST_KEY_TRANSFER_OWNER = "request_key_transfer_owner"
        const val RESULT_TEAMSPACE_ID = "result_teamspace_id"
        const val RESULT_NEW_OWNER_ID = "result_new_owner_id"
        const val RESULT_NEW_OWNER_NAME = "result_new_owner_name"

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
