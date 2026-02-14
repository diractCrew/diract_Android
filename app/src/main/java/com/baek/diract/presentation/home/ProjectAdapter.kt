package com.baek.diract.presentation.home

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.baek.diract.R
import com.baek.diract.databinding.ItemProjectBinding
import com.baek.diract.domain.model.ProjectSummary
import com.baek.diract.domain.model.SongListSummary
import com.baek.diract.presentation.common.CustomToast
import com.baek.diract.presentation.common.MaxLengthInputFilter

class ProjectAdapter(
    private val onLongClick: (view: View, project: ProjectSummary) -> Unit,
    private val onClick: (project: ProjectSummary) -> Unit,
    private val onEditTextChanged: (String) -> Unit,
    private val onAddSongList: (ProjectSummary) -> Unit,
    private val onOpenSongs: (anchor: View, project: ProjectSummary) -> Unit,
    private val onSongListClick: (ProjectSummary, SongListSummary) -> Unit,
    private val onSongListDelete: (ProjectSummary, SongListSummary) -> Unit,
    private val onSongListRename: (ProjectSummary, SongListSummary, String) -> Unit,
    private val onSongListEditStateChanged: (isEditing: Boolean) -> Unit
) : ListAdapter<ProjectSummary, ProjectAdapter.VH>(DIFF) {

    private var expandedProjectId: String? = null

    // 프로젝트 이름(프로젝트 자체) 편집 상태
    private var editingProjectId: String? = null
    private var editingDraft: String = ""

    // 프로젝트별 곡리스트 데이터
    private val songListsByProjectId = mutableMapOf<String, List<SongListSummary>>()

    // ✅ 현재 “곡 리스트 이름 편집” 중인 SongListAdapter를 잡아두는 포인터
    private var activeSongEditor: SongListAdapter? = null

    /** ✅ HomeFragment의 체크 버튼에서 호출: 곡 리스트 편집 저장+종료 */
    fun commitActiveSongEditAndExit(): Boolean {
        return activeSongEditor?.commitEditAndExit() == true
    }

    /** 프로젝트 이름(프로젝트) 편집 시작 */
    fun setEditMode(projectId: String, currentName: String) {
        editingProjectId = projectId
        editingDraft = currentName
        notifyDataSetChanged()
    }

    /** 프로젝트 이름(프로젝트) 편집 종료 */
    fun clearEditMode() {
        editingProjectId = null
        editingDraft = ""
        notifyDataSetChanged()
    }

    fun setSongLists(projectId: String, lists: List<SongListSummary>) {
        songListsByProjectId[projectId] = lists
        val pos = currentList.indexOfFirst { it.id == projectId }
        if (pos != -1) notifyItemChanged(pos)
    }

    fun toggleExpanded(projectId: String) {
        val prevId = expandedProjectId
        expandedProjectId = if (prevId == projectId) null else projectId

        prevId?.let { id ->
            val prevPos = currentList.indexOfFirst { it.id == id }
            if (prevPos != -1) notifyItemChanged(prevPos)
        }
        expandedProjectId?.let { id ->
            val newPos = currentList.indexOfFirst { it.id == id }
            if (newPos != -1) notifyItemChanged(newPos)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemProjectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(val binding: ItemProjectBinding) : RecyclerView.ViewHolder(binding.root) {

        private var ignoreChange = false
        private var watcher: TextWatcher? = null
        private var boundProject: ProjectSummary? = null

        private val songListAdapter: SongListAdapter

        init {
            songListAdapter = SongListAdapter(
                onClick = { song ->
                    boundProject?.let { project -> onSongListClick(project, song) }
                },
                onDelete = { song ->
                    boundProject?.let { project -> onSongListDelete(project, song) }
                },
                onRename = { song, newName ->
                    boundProject?.let { project -> onSongListRename(project, song, newName) }
                },
                onEditStateChanged = { isEditing ->
                    // (1) 곡추가 버튼 토글
                    binding.btnAddSong.visibility = if (isEditing) View.GONE else View.VISIBLE

                    // (2) 현재 편집중인 SongListAdapter 포인터 등록/해제
                    if (isEditing) {
                        activeSongEditor = songListAdapter
                    } else {
                        if (activeSongEditor === songListAdapter) activeSongEditor = null
                    }

                    // (3) HomeFragment 상단바 토글
                    onSongListEditStateChanged(isEditing)
                }
            )

            binding.rvSongs.apply {
                adapter = songListAdapter
                isNestedScrollingEnabled = false
                setHasFixedSize(false)
            }
        }

        fun bind(item: ProjectSummary) = with(binding) {
            boundProject = item
            val isEditingProject = (item.id == editingProjectId)

            // 프로젝트 편집 모드 UI 토글
            tvProjectName.visibility = if (isEditingProject) View.GONE else View.VISIBLE
            ivOpenSongs.visibility = if (isEditingProject) View.GONE else View.VISIBLE
            cardProjectEditMode.visibility = if (isEditingProject) View.VISIBLE else View.GONE

            if (!isEditingProject) {
                // ---------- 보기 모드 ----------
                tvProjectName.text = item.name

                val expanded = (item.id == expandedProjectId)
                cardExpanded.visibility = if (expanded) View.VISIBLE else View.GONE
                ivOpenSongs.rotation = if (expanded) 90f else 0f

                if (expanded) {
                    val lists = songListsByProjectId[item.id] ?: emptyList()
                    val hasSongList = lists.isNotEmpty()

                    btnAddSong.backgroundTintList = ContextCompat.getColorStateList(
                        root.context,
                        if (hasSongList) R.color.fill_assistive else R.color.secondary_strong
                    )
                    btnAddSong.setOnClickListener {
                        boundProject?.let { project -> onAddSongList(project) }
                    }
                    btnAddSong.visibility = View.VISIBLE
                    tvSongEmpty.visibility = if (hasSongList) View.GONE else View.VISIBLE
                    rvSongs.visibility = if (hasSongList) View.VISIBLE else View.GONE

                    if (hasSongList) {
                        songListAdapter.submitList(lists)
                    }
                }

                // 리스너/와처 정리 (재활용 대응)
                watcher?.let { etProjectName.removeTextChangedListener(it) }
                watcher = null

                // 카드 클릭: 펼치기/접기
                cardProjectItem.setOnClickListener { toggleExpanded(item.id) }

                // 롱클릭: 옵션
                cardProjectItem.setOnLongClickListener { v -> onLongClick(v, item); true }

                // (필요 시) 단일 클릭 콜백
                // cardProjectItem.setOnClickListener { onClick(item); toggleExpanded(item.id) }
                return@with
            }

            // ---------- 프로젝트 이름 편집 모드 ----------
            cardProjectItem.setOnClickListener(null)
            cardProjectItem.setOnLongClickListener(null)

            // 1) 기존 와처 제거
            watcher?.let { etProjectName.removeTextChangedListener(it) }

            // 2) 20자 제한 + 토스트
            etProjectName.filters = arrayOf(
                MaxLengthInputFilter(20) {
                    CustomToast.showNegative(
                        root.context,
                        root.context.getString(R.string.song_list_name_max_warning)
                    )
                }
            )

            // 3) 초기 텍스트 세팅
            if (etProjectName.text.toString() != editingDraft) {
                ignoreChange = true
                etProjectName.setText(editingDraft)
                etProjectName.setSelection(editingDraft.length)
                ignoreChange = false
            }
            tvProjectEditCounter.text = root.context.getString(
                R.string.input_dialog_counter,
                editingDraft.length,
                20
            )

            // 4) 텍스트 변경 감시
            watcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (ignoreChange) return
                    val text = s?.toString().orEmpty()
                    editingDraft = text

                    tvProjectEditCounter.text = root.context.getString(
                        R.string.input_dialog_counter,
                        text.length,
                        20
                    )
                    tvProjectEditCounter.setTextColor(
                        ContextCompat.getColor(root.context, R.color.label_assistive)
                    )

                    onEditTextChanged(text) // HomeFragment에서 draft 받기
                }
            }.also { etProjectName.addTextChangedListener(it) }

            // 5) X 버튼: 전체 삭제
            ivProjectEditClear.setOnClickListener {
                etProjectName.setText("")
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ProjectSummary>() {
            override fun areItemsTheSame(oldItem: ProjectSummary, newItem: ProjectSummary) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: ProjectSummary, newItem: ProjectSummary) =
                oldItem == newItem
        }
    }
}
