package com.baek.diract.presentation.common.option

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.recyclerview.widget.LinearLayoutManager
import com.baek.diract.databinding.PopupOptionBinding

/**
 * 옵션 선택 팝업 메뉴
 *
 *      - Long press 시 앵커 뷰 근처에 표시되는 작은 옵션 메뉴 컴포넌트
 *      - 위치 및 크기, 텍스트 스타일 등은 모두 구현해놓음 -> 따로 설정할 필요 X
 *      - dismiss(팝업 없애기)도 따로 설정할 필요 X
 *          - 팝업 바깥부분 클릭 시, 옵션 선택 시 사라짐
 *
 * 파일 하단의 companion object와 아래 사용예시를 참고해서 사용
 *      - 각 동작(삭제, 수정 등)은 OptionItem.kt 참고
 *      - id를 기준으로 동작 분기하면 됨
 *
 * 사용 예시:
 * //Long Press(꾹 누르는 제스처)에서 사용 시
 * itemView.setOnLongClickListener { view ->
 *      OptionPopup
 *             .basicOptions(requireContext(), onOptionSelected = ::onOptionSelected)
 *             .show(view)
 *      true
 * }
 * private fun onOptionSelected(item: OptionItem) {
 *      if(item.id == OptionItem.ID_DELETE){
 *          //옵션 선택 시 동작 정의
 *      }
 * }
 *
 */

class OptionPopup private constructor(
    private val context: Context,
    private val options: List<OptionItem>,
    private val onOptionSelected: ((OptionItem) -> Unit)?
) {
    private var popupWindow: PopupWindow? = null

    /**
     * 앵커 뷰 근처에 팝업 표시
     *
     * @param anchor 팝업이 표시될 기준 뷰
     */
    fun show(anchor: View) {
        val binding = PopupOptionBinding.inflate(LayoutInflater.from(context))

        // RecyclerView 설정
        binding.rvOptions.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = OptionAdapter(options) { option ->
                onOptionSelected?.invoke(option)
                dismiss()
            }
        }

        // PopupWindow 생성
        popupWindow = PopupWindow(
            binding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true // focusable - 바깥 터치시 닫힘
        ).apply {
            elevation = 8f
            // 바깥 영역 터치시 닫힘
            isOutsideTouchable = true
        }

        // 팝업 위치 계산 및 표시
        binding.root.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        val popupWidth = binding.root.measuredWidth
        val popupHeight = binding.root.measuredHeight

        // 앵커 뷰 위치 가져오기
        val location = IntArray(2)
        anchor.getLocationOnScreen(location)
        val anchorX = location[0]
        val anchorY = location[1]

        // 화면 크기
        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight = context.resources.displayMetrics.heightPixels

        // X 위치: 앵커 오른쪽 끝 기준 정렬
        var x = anchorX + anchor.width - popupWidth
        x = x.coerceIn(16, screenWidth - popupWidth - 16)

        // Y 위치: 앵커 아래에 표시, 공간이 없으면 위에 표시
        val spaceAbove = anchorY
        val spaceBelow = screenHeight - anchorY - anchor.height
        val margin = 8

        val y = if (spaceBelow >= popupHeight + margin) {
            // 아래에 표시
            anchorY + anchor.height + margin
        } else {
            // 위에 표시
            anchorY - popupHeight - margin
        }

        popupWindow?.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y)
    }

    //팝업 닫기
    fun dismiss() {
        popupWindow?.dismiss()
        popupWindow = null
    }

    //팝업이 띄워져 있는지
    fun isShowing(): Boolean = popupWindow?.isShowing == true

    //팝업 빌더
    class Builder(private val context: Context) {
        private val options = mutableListOf<OptionItem>()
        private var onOptionSelected: ((OptionItem) -> Unit)? = null

        //옵션 추가
        fun addOption(option: OptionItem): Builder {
            options.add(option)
            return this
        }

        // 여러 옵션 추가
        fun addOptions(vararg optionList: OptionItem): Builder {
            options.addAll(optionList)
            return this
        }

        // 옵션 선택 리스너 설정
        fun setOnOptionSelectedListener(listener: (OptionItem) -> Unit): Builder {
            onOptionSelected = listener
            return this
        }

        // 옵션 팝업 빌드
        fun build(): OptionPopup {
            return OptionPopup(
                context = context,
                options = options.toList(),
                onOptionSelected = onOptionSelected
            )
        }

        // 옵션 팝업 및 빌드
        fun show(anchor: View): OptionPopup {
            val popup = build()
            popup.show(anchor)
            return popup
        }
    }

    companion object {

        fun builder(context: Context): Builder = Builder(context)

        // ===== 디자인 되어있는 옵션 프리셋 =====

        //기본 옵션(이름 수정, 삭제)
        fun basicOptions(context: Context, onOptionSelected: (OptionItem) -> Unit): Builder {
            return builder(context)
                .addOption(OptionItem.editName())
                .addOption(OptionItem.delete())
                .setOnOptionSelectedListener(onOptionSelected)
        }

        //비디오 옵션 (이름 수정, 다른 파트로 이동, 삭제)
        fun videoOptions(context: Context, onOptionSelected: (OptionItem) -> Unit): Builder {
            return builder(context)
                .addOption(OptionItem.editName())
                .addOption(OptionItem.movePart())
                .addOption(OptionItem.delete())
                .setOnOptionSelectedListener(onOptionSelected)
        }

        fun reportOptions(context: Context, onOptionSelected: (OptionItem) -> Unit): Builder {
            return builder(context)
                .addOption(OptionItem.report())
                .setOnOptionSelectedListener(onOptionSelected)
        }

        fun feedbackAuthorOptions(
            context: Context,
            onOptionSelected: (OptionItem) -> Unit
        ): Builder {
            return builder(context)
                .addOption(OptionItem.editFeedback())
                .addOption(OptionItem.delete())
                .setOnOptionSelectedListener(onOptionSelected)
        }

        fun feedbackOptions(context: Context, onOptionSelected: (OptionItem) -> Unit): Builder {
            return builder(context)
                .addOption(OptionItem.report())
                .setOnOptionSelectedListener(onOptionSelected)
        }

        fun commentAuthorOptions(
            context: Context,
            onOptionSelected: (OptionItem) -> Unit
        ): Builder {
            return builder(context)
                .addOption(OptionItem.editComment())
                .addOption(OptionItem.delete())
                .setOnOptionSelectedListener(onOptionSelected)
        }

        fun commentOptions(context: Context, onOptionSelected: (OptionItem) -> Unit): Builder {
            return builder(context)
                .addOption(OptionItem.report())
                .setOnOptionSelectedListener(onOptionSelected)
        }
    }
}
