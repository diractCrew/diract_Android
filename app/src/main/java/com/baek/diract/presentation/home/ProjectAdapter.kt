package com.baek.diract.presentation.home

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.baek.diract.R
import com.baek.diract.databinding.ItemProjectBinding
import com.baek.diract.domain.model.ProjectSummary
import com.baek.diract.domain.model.TracksSummary
import com.baek.diract.presentation.common.CustomToast
import com.baek.diract.presentation.common.MaxLengthInputFilter

class ProjectAdapter(
    private val onLongClick: (view: View, project: ProjectSummary) -> Unit,
    private val onClick: (project: ProjectSummary) -> Unit,
    private val onEditTextChanged: (String) -> Unit,

    // ✅ tracks
    private val onAddTracks: (ProjectSummary) -> Unit,
    private val onOpenTracks: (anchor: View, project: ProjectSummary) -> Unit,
    private val onRetryTracks: (project: ProjectSummary) -> Unit, // ✅ 실패 시 재시도
    private val onTracksClick: (ProjectSummary, TracksSummary) -> Unit,
    private val onTracksDelete: (ProjectSummary, TracksSummary) -> Unit,
    private val onTracksRename: (ProjectSummary, TracksSummary, String) -> Unit,
    private val onTracksEditStateChanged: (isEditing: Boolean) -> Unit
) : ListAdapter<ProjectSummary, ProjectAdapter.VH>(DIFF) {

    private var tracksCountByProjectId: Map<String, Int> = emptyMap()
    private var expandedProjectId: String? = null

    // 프로젝트 이름 편집 상태
    private var editingProjectId: String? = null
    private var editingDraft: String = ""

    // ✅ 프로젝트별 tracks 데이터 (HomeFragment가 setTracks로 주입)
    private val tracksByProjectId = mutableMapOf<String, List<TracksSummary>>()

    // ✅ 프로젝트별 tracks 로딩 상태
    private enum class TracksLoadState { IDLE, LOADING, SUCCESS, ERROR }
    private val tracksStateByProjectId = mutableMapOf<String, TracksLoadState>()

    // ✅ 현재 “tracks 이름 편집” 중인 TracksAdapter
    private var activeTracksEditor: TracksAdapter? = null

    fun commitActiveTracksEditAndExit(): Boolean {
        return activeTracksEditor?.commitEditAndExit() == true
    }

    fun setEditMode(projectId: String, currentName: String) {
        editingProjectId = projectId
        editingDraft = currentName
        notifyDataSetChanged()
    }
    fun setTracksCount(map: Map<String, Int>) {
        tracksCountByProjectId = map
        notifyDataSetChanged()
    }
    fun clearEditMode() {
        editingProjectId = null
        editingDraft = ""
        notifyDataSetChanged()
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

    /** ✅ HomeFragment가 서버에서 받아온 tracks 주입 */
    fun setTracks(projectId: String, lists: List<TracksSummary>) {
        tracksByProjectId[projectId] = lists
        tracksStateByProjectId[projectId] = TracksLoadState.SUCCESS
        val pos = currentList.indexOfFirst { it.id == projectId }
        if (pos != -1) notifyItemChanged(pos)
    }

    fun setTracksLoading(projectId: String, loading: Boolean) {
        tracksStateByProjectId[projectId] =
            if (loading) TracksLoadState.LOADING else TracksLoadState.SUCCESS
        val pos = currentList.indexOfFirst { it.id == projectId }
        if (pos != -1) notifyItemChanged(pos)
    }

    fun setTracksError(projectId: String) {
        tracksStateByProjectId[projectId] = TracksLoadState.ERROR
        val pos = currentList.indexOfFirst { it.id == projectId }
        if (pos != -1) notifyItemChanged(pos)
    }

    private fun getTracksState(projectId: String): TracksLoadState {
        return tracksStateByProjectId[projectId] ?: TracksLoadState.IDLE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemProjectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(private val binding: ItemProjectBinding) : RecyclerView.ViewHolder(binding.root) {

        private var ignoreChange = false
        private var watcher: TextWatcher? = null
        private var boundProject: ProjectSummary? = null

        private val tracksAdapter: TracksAdapter = TracksAdapter(
            onClick = { track -> boundProject?.let { p -> onTracksClick(p, track) } },
            onDelete = { track -> boundProject?.let { p -> onTracksDelete(p, track) } },
            onRename = { track, newName -> boundProject?.let { p -> onTracksRename(p, track, newName) } },
            onEditStateChanged = { isEditing ->
                binding.btnAddSong.visibility = if (isEditing) View.GONE else View.VISIBLE

                if (isEditing) activeTracksEditor = tracksAdapter
                else if (activeTracksEditor === tracksAdapter) activeTracksEditor = null

                onTracksEditStateChanged(isEditing)
            }
        )

        init {
            binding.rvSongs.apply {
                adapter = tracksAdapter
                layoutManager = object : LinearLayoutManager(context) {
                    override fun canScrollVertically() = false
                }
                isNestedScrollingEnabled = false
                setHasFixedSize(false)
                overScrollMode = RecyclerView.OVER_SCROLL_NEVER
                isFocusable = false
                isFocusableInTouchMode = false
            }
        }

        fun bind(item: ProjectSummary) = with(binding) {
            boundProject = item
            val isEditingProject = (item.id == editingProjectId)

            tvProjectName.visibility = if (isEditingProject) View.GONE else View.VISIBLE
            ivOpenSongs.visibility = if (isEditingProject) View.GONE else View.VISIBLE
            cardProjectEditMode.visibility = if (isEditingProject) View.VISIBLE else View.GONE

            if (!isEditingProject) {
                tvProjectName.text = item.name

                val expanded = (item.id == expandedProjectId)
                cardExpanded.visibility = if (expanded) View.VISIBLE else View.GONE
                ivOpenSongs.rotation = if (expanded) 90f else 0f

                cardProjectItem.setOnClickListener {
                    val willExpand = (expandedProjectId != item.id)
                    toggleExpanded(item.id)
                    if (willExpand) onOpenTracks(root, item)
                    onClick(item)
                }
                cardProjectItem.setOnLongClickListener { v ->
                    onLongClick(v, item)
                    true
                }

                if (expanded) {
                    val state = getTracksState(item.id)
                    val tracks = tracksByProjectId[item.id] ?: emptyList()
                    val hasTracks = tracks.isNotEmpty()

                    // 버튼은 항상 보여주되, 로딩 중엔 비활성
                    btnAddSong.visibility = View.VISIBLE
                    btnAddSong.isEnabled = (state != TracksLoadState.LOADING)

                    btnAddSong.backgroundTintList = ContextCompat.getColorStateList(
                        root.context,
                        if (hasTracks) R.color.fill_assistive else R.color.secondary_strong
                    )
                    btnAddSong.setOnClickListener { boundProject?.let(onAddTracks) }

                    // ✅ 상태 UI 토글
                    when (state) {
                        TracksLoadState.LOADING -> {
                            pbSongLoading.visibility = View.VISIBLE
                            tvSongError.visibility = View.GONE
                            tvSongEmpty.visibility = View.GONE
                            rvSongs.visibility = View.GONE
                            tracksAdapter.submitList(emptyList())
                        }

                        TracksLoadState.ERROR -> {
                            pbSongLoading.visibility = View.GONE
                            tvSongError.visibility = View.VISIBLE
                            tvSongEmpty.visibility = View.GONE
                            rvSongs.visibility = View.GONE
                            tracksAdapter.submitList(emptyList())

                            // ✅ “재시도” 클릭
                            tvSongError.setOnClickListener { boundProject?.let(onRetryTracks) }
                        }

                        else -> {
                            pbSongLoading.visibility = View.GONE
                            tvSongError.visibility = View.GONE
                            tvSongEmpty.visibility = if (hasTracks) View.GONE else View.VISIBLE
                            rvSongs.visibility = if (hasTracks) View.VISIBLE else View.GONE
                            tracksAdapter.submitList(tracks)
                        }
                    }
                } else {
                    // 접힘 상태는 항상 비움 (재활용 방지)
                    pbSongLoading.visibility = View.GONE
                    tvSongError.visibility = View.GONE
                    tvSongEmpty.visibility = View.GONE
                    rvSongs.visibility = View.GONE
                    tracksAdapter.submitList(emptyList())
                }

                watcher?.let { etProjectName.removeTextChangedListener(it) }
                watcher = null
                return@with
            }

            // ---- 프로젝트 이름 편집 모드 ----
            cardProjectItem.setOnClickListener(null)
            cardProjectItem.setOnLongClickListener(null)

            watcher?.let { etProjectName.removeTextChangedListener(it) }

            etProjectName.filters = arrayOf(
                MaxLengthInputFilter(20) {
                    CustomToast.showNegative(
                        root.context,
                        root.context.getString(R.string.song_list_name_max_warning)
                    )
                }
            )

            if (etProjectName.text.toString() != editingDraft) {
                ignoreChange = true
                etProjectName.setText(editingDraft)
                etProjectName.setSelection(editingDraft.length)
                ignoreChange = false
            }

            tvProjectEditCounter.text = root.context.getString(
                R.string.input_dialog_counter, editingDraft.length, 20
            )

            watcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (ignoreChange) return
                    val text = s?.toString().orEmpty()
                    editingDraft = text

                    tvProjectEditCounter.text = root.context.getString(
                        R.string.input_dialog_counter, text.length, 20
                    )

                    onEditTextChanged(text)
                }
            }.also { etProjectName.addTextChangedListener(it) }

            ivProjectEditClear.setOnClickListener { etProjectName.setText("") }
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
