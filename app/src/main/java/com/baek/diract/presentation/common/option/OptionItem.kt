package com.baek.diract.presentation.common.option

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.baek.diract.R

/**
 * 옵션 메뉴 아이템 데이터 클래스
 *
 * @param id 옵션 식별자 (클릭 시 어떤 옵션인지 구분)
 * @param title 옵션 텍스트
 * @param textColorRes 텍스트 색상 리소스 (기본: label_strong, 삭제 등 위험 액션: accent_red_normal)
 */
data class OptionItem(
    val id: String,
    @field:StringRes val titleRes: Int,
    @field:ColorRes val textColorRes: Int = R.color.label_strong
) {
    companion object {
        // 자주 사용하는 옵션 ID 상수
        const val ID_EDIT_NAME = "edit_name"
        const val ID_MOVE = "move"
        const val ID_DELETE = "delete"
        const val ID_REPORT = "report"
        const val ID_EDIT_FEEDBACK = "edit_feedback"

        //이름 수정
        fun editName() = OptionItem(
            id = ID_EDIT_NAME,
            titleRes = R.string.edit_name_option
        )

        //다른 파트로 이동
        fun movePart() = OptionItem(
            id = ID_MOVE,
            titleRes = R.string.move_part_option
        )

        //삭제
        fun delete() = OptionItem(
            id = ID_DELETE,
            titleRes = R.string.delete_option,
            textColorRes = R.color.accent_red_normal
        )

        //신고
        fun report() = OptionItem(
            id = ID_REPORT,
            titleRes = R.string.report_option,
            textColorRes = R.color.accent_red_normal
        )

        //피드백 수정
        fun editFeedback() = OptionItem(
            id = ID_EDIT_FEEDBACK,
            titleRes = R.string.edit_feedback_option
        )
    }
}
