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

    // ✅ arguments 키를 상수로 통일해서 읽기
    private val memberId by lazy { requireArguments().getString(ARG_ID).orEmpty() }
    private val memberName by lazy { requireArguments().getString(ARG_NAME).orEmpty() }

    // ✅ Fragment로 전달할 콜백
    private var onKickClick: ((String, String) -> Unit)? = null

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
            renderLeaderState(LeaderState.LOADING)

            view.postDelayed({
                val success = true
                if (success) {
                    renderLeaderState(LeaderState.SUCCESS)
                } else {
                    Snackbar.make(requireView(), "팀장 권한 주기를 실패했습니다.", Snackbar.LENGTH_SHORT).show()
                    renderLeaderState(LeaderState.IDLE)
                }
            }, 800)
        }

        // ✅ 여기만 핵심 변경: 이미 계산된 memberId/memberName 그대로 콜백
        tvKick.setOnClickListener {
            dismiss()
            onKickClick?.invoke(memberId, memberName)
        }
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
        private const val ARG_ID = "arg_id"
        private const val ARG_NAME = "arg_name"

        fun newInstance(
            memberId: String,
            memberName: String,
            onKickClick: (String, String) -> Unit
        ): MemberActionBottomSheet {
            return MemberActionBottomSheet().apply {
                arguments = bundleOf(
                    ARG_ID to memberId,
                    ARG_NAME to memberName
                )
                this.onKickClick = onKickClick
            }

        }

    }
}
