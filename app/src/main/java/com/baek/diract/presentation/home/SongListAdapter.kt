package com.baek.diract.presentation.home

import android.text.Editable
import android.text.InputFilter
import android.text.Spanned
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.baek.diract.R
import com.baek.diract.databinding.ItemSongListBinding
import com.baek.diract.domain.model.SongListSummary
import com.baek.diract.presentation.common.CustomToast
import com.baek.diract.presentation.common.MaxLengthInputFilter
import com.baek.diract.presentation.common.option.OptionItem
import com.baek.diract.presentation.common.option.OptionPopup

class SongListAdapter(
    private val onClick: (SongListSummary) -> Unit,
    private val onDelete: (SongListSummary) -> Unit,
    private val onRename: (SongListSummary, String) -> Unit,
    private val onEditStateChanged: (isEditing: Boolean) -> Unit,
) : ListAdapter<SongListSummary, SongListAdapter.VH>(DIFF) {

    private var editingId: String? = null
    private var editingDraft: String = ""

    fun setEditMode(id: String, current: String) {
        editingId = id
        editingDraft = current
        onEditStateChanged(true)
        notifyDataSetChanged()
    }

    fun clearEditMode() {
        editingId = null
        editingDraft = ""
        onEditStateChanged(false)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemSongListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }
    fun commitEditAndExit(): Boolean {
        val id = editingId ?: return false

        val newName = editingDraft.trim()
        if (newName.isNotEmpty()) {
            // 현재 리스트에서 편집 중 아이템 찾아서 onRename 호출
            val item = currentList.firstOrNull { it.id == id }
            if (item != null) onRename(item, newName)
        }

        // 편집 종료 + 상단바 복구 콜백(onEditStateChanged(false))도 여기서 같이 나감
        clearEditMode()
        return true
    }
    inner class VH(
        private val binding: ItemSongListBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var watcher: TextWatcher? = null
        private var ignoreChange = false

        fun bind(item: SongListSummary) = with(binding) {
            val isEditing = (item.id == editingId)

            // ✅ 2카드 토글(새 XML 기준)
            cardViewMode.visibility = if (isEditing) View.GONE else View.VISIBLE
            cardEditSongMode.visibility = if (isEditing) View.VISIBLE else View.GONE

            // ---------- 보기 모드 ----------
            tvTitle.text = item.title
            tvCount.text = item.projectCount.toString()

            // 리스너 초기화(재활용 대응)
            cardViewMode.setOnClickListener(null)
            cardViewMode.setOnLongClickListener(null)

            if (!isEditing) {
                // 클릭: 상세 화면 이동
                cardViewMode.setOnClickListener { onClick(item) }

                // 롱클릭: 옵션 팝업(이름 수정/삭제)
                cardViewMode.setOnLongClickListener { view ->
                    OptionPopup
                        .basicOptions(view.context) { option: OptionItem ->
                            when (option.id) {
                                OptionItem.ID_EDIT_NAME -> setEditMode(item.id, item.title)
                                OptionItem.ID_DELETE -> onDelete(item)
                            }
                        }
                        .show(view)
                    true
                }

                // 보기 모드에서는 watcher 정리
                watcher?.let { etEditSongTitle.removeTextChangedListener(it) }
                watcher = null

                return@with
            }

            // ---------- 편집 모드 ----------
            // watcher 제거(재활용 대응)
            watcher?.let { etEditSongTitle.removeTextChangedListener(it) }
            watcher = null

            // 기본 색(정상 상태)로 초기화
            fun setNormalUi(len: Int) {
                tvEditCounter.text = root.context.getString(R.string.input_dialog_counter, len, 20)
                tvEditCounter.setTextColor(ContextCompat.getColor(root.context, R.color.label_assistive))
                vEditUnderline.setBackgroundColor(ContextCompat.getColor(root.context, R.color.secondary_normal))
            }

            fun setErrorUi() {
                tvEditCounter.setTextColor(ContextCompat.getColor(root.context, R.color.accent_red_normal))
                vEditUnderline.setBackgroundColor(ContextCompat.getColor(root.context, R.color.accent_red_normal))
            }

            // 20자 제한(초과 입력 "시도" 시)
            etEditSongTitle.filters = arrayOf(
                MaxLengthInputFilter(20) {
                    setErrorUi()
                    CustomToast.showNegative(
                        root.context,
                        root.context.getString(R.string.song_list_name_max_warning)
                    )
                }
            )

            // 초기값 세팅
            val current = etEditSongTitle.text?.toString().orEmpty()
            if (current != editingDraft) {
                ignoreChange = true
                etEditSongTitle.setText(editingDraft)
                etEditSongTitle.setSelection(editingDraft.length)
                ignoreChange = false
            }
            setNormalUi(editingDraft.length)

            // clear 버튼
            ivEditSongClear.setOnClickListener {
                etEditSongTitle.text?.clear()
            }

            // 입력 변화 반영
            val tw = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (ignoreChange) return
                    editingDraft = s?.toString().orEmpty()
                    // MaxLengthInputFilter가 20자 초과를 막으니 기본은 정상 UI로 유지
                    setNormalUi(editingDraft.length)
                }
            }
            etEditSongTitle.addTextChangedListener(tw)
            watcher = tw

            // Done → 저장
            etEditSongTitle.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    commitEditAndExit()
                    true
                } else false
            }
        }
    }

    private class LengthFilterWithToast(
        private val max: Int,
        private val onOverflow: () -> Unit
    ) : InputFilter {
        override fun filter(
            source: CharSequence?,
            start: Int,
            end: Int,
            dest: Spanned?,
            dstart: Int,
            dend: Int
        ): CharSequence? {
            val keep = max - ((dest?.length ?: 0) - (dend - dstart))
            if (keep >= end - start) return null

            onOverflow()
            return if (keep <= 0) "" else source?.subSequence(start, start + keep).toString()
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<SongListSummary>() {
            override fun areItemsTheSame(oldItem: SongListSummary, newItem: SongListSummary) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: SongListSummary, newItem: SongListSummary) =
                oldItem == newItem
        }
    }
}
