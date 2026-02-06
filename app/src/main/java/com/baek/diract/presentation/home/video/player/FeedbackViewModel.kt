package com.baek.diract.presentation.home.video.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baek.diract.R
import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.model.Feedback
import com.baek.diract.domain.model.FeedbackUser
import com.baek.diract.domain.repository.AuthRepository
import com.baek.diract.domain.repository.FeedbackRepository
import com.baek.diract.presentation.common.ToastEvent
import com.baek.diract.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FeedbackInputState {
    DEFAULT, SELECTING_RANGE, WRITING_COMMENT
}

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val feedbackRepository: FeedbackRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val videoId: String = checkNotNull(savedStateHandle[KEY_VIDEO_ID]) {
        "videoId값 없이 플레이어에 접근이 불가능합니다."
    }

    private val uid get() = authRepository.getCurrentUser()?.uid
    private val teamspaceId: String = "teamspaceId" //TODO: TeamspaceRepository에서 가져오기

    // 피드백 목록 상태
    private val _feedbackState = MutableStateFlow<UiState<Long>>(UiState.None)
    val feedbackState: StateFlow<UiState<Long>> = _feedbackState.asStateFlow()
    private val feedbacks = MutableStateFlow<List<Feedback>>(emptyList())

    private val _feedbackItem = MutableStateFlow<List<FeedbackItem>>(emptyList())
    val feedbackItem: StateFlow<List<FeedbackItem>> = _feedbackItem.asStateFlow()

    // 피드백 입력 상태
    private val _feedbackInputState = MutableStateFlow(FeedbackInputState.DEFAULT)
    val feedbackInputState: StateFlow<FeedbackInputState> = _feedbackInputState.asStateFlow()

    private var _rangeStartTime: Long = 0L
    val rangeStartTime: Long get() = _rangeStartTime

    private var _rangeEndTime: Long? = null
    val rangeEndTime: Long? get() = _rangeEndTime

    private var _editingFeedback: FeedbackItem? = null
    val editingFeedback: FeedbackItem? get() = _editingFeedback

    private var _isMentioning: Boolean = false
    val isMentioning: Boolean get() = _isMentioning

    // 팀 멤버 목록 (TODO: TeamspaceRepository에서 가져오기)
    private val _teamMembers = MutableStateFlow<List<FeedbackUser>>(emptyList())

    // 선택된 멘션 목록
    private val _selectedMentions = MutableStateFlow<List<FeedbackUser>>(emptyList())
    val selectedMentions: StateFlow<List<FeedbackUser>> = _selectedMentions.asStateFlow()

    private val _toastMessage = MutableSharedFlow<ToastEvent>()
    val toastMessage: SharedFlow<ToastEvent> = _toastMessage.asSharedFlow()

    init {
        loadFeedbacks()
        loadTeamMembers()
    }

    /*
        피드백 입력 상태 관리
     */

    // 시점 피드백 시작
    fun startPointFeedback(currentTimeMs: Long) {
        _rangeStartTime = currentTimeMs
        _rangeEndTime = null
        _feedbackInputState.value = FeedbackInputState.WRITING_COMMENT
    }

    // 구간 피드백 시작
    fun startIntervalFeedback(currentTimeMs: Long) {
        _rangeStartTime = currentTimeMs
        _feedbackInputState.value = FeedbackInputState.SELECTING_RANGE
    }

    // 구간 선택 완료
    fun completeRangeSelection(currentTimeMs: Long) {
        if(_rangeStartTime > currentTimeMs){
            viewModelScope.launch {
                _toastMessage.emit(ToastEvent(R.string.toast_set_time_after_start, true))
            }
            return
        }
        _rangeEndTime = currentTimeMs
        _feedbackInputState.value = FeedbackInputState.WRITING_COMMENT
    }

    // 피드백 수정 시작
    fun startEditFeedback(feedback: FeedbackItem) {
        clearMentions()
        feedback.taggedUsers.forEach { addMention(it) }
        _editingFeedback = feedback
        _rangeStartTime = (feedback.startTime * 1000).toLong()
        _rangeEndTime = feedback.endTime?.let { (it * 1000).toLong() }
        _isMentioning = false
        _feedbackInputState.value = FeedbackInputState.WRITING_COMMENT
    }

    // 피드백 입력 상태 초기화
    fun resetFeedbackInput() {
        _feedbackInputState.value = FeedbackInputState.DEFAULT
        _editingFeedback = null
        _rangeStartTime = 0L
        _rangeEndTime = null
        _isMentioning = false
        clearMentions()
    }

    fun setMentioning(mentioning: Boolean) {
        _isMentioning = mentioning
    }

    // 피드백 전송 (새 작성 / 수정 통합)
    fun submitFeedback(content: String) {
        val editing = _editingFeedback
        if (editing != null) {
            editFeedback(editing.feedbackId, content)
        } else {
            val startTimeSec = _rangeStartTime / 1000.0
            val endTimeSec = _rangeEndTime?.let { it / 1000.0 }
            uploadFeedback(content = content, startTime = startTimeSec, endTime = endTimeSec)
        }
        resetFeedbackInput()
    }

    /*
        멘션 관련
     */

    // 팀 멤버 로드 (TODO: 실제 Repository에서 가져오기)
    private fun loadTeamMembers() {
        _teamMembers.value = listOf(
            FeedbackUser(userId = "1", name = "리비"),
            FeedbackUser(userId = "2", name = "벨코"),
            FeedbackUser(userId = "3", name = "파이디온"),
            FeedbackUser(userId = "4", name = "죠리애"),
            FeedbackUser(userId = "5", name = "죠리애"),
            FeedbackUser(userId = "6", name = "죠리애")
        )
    }

    // 멘션 필터링 (@All 포함)
    fun filterMentionMembers(query: String): List<FeedbackUser> {
        val allMember = FeedbackUser(userId = ALL_MEMBER_ID, name = "All")
        val members = _teamMembers.value

        if (query.isEmpty()) {
            return listOf(allMember) + members
        }

        val filtered = members.filter { member ->
            member.name?.contains(query, ignoreCase = true) == true
        }

        val allMatches = "All".contains(query, ignoreCase = true)
        return if (allMatches) {
            listOf(allMember) + filtered
        } else {
            filtered
        }
    }

    fun addMention(member: FeedbackUser) {
        if (member.userId == ALL_MEMBER_ID) {
            _selectedMentions.value = _teamMembers.value.toList()
            return
        }
        if (_selectedMentions.value.any { it.userId == member.userId }) return
        _selectedMentions.value += member
    }

    fun isAllMembersSelected(): Boolean {
        val members = _teamMembers.value
        if (members.isEmpty()) return false
        return members.all { member ->
            _selectedMentions.value.any { it.userId == member.userId }
        }
    }

    fun removeMention(userId: String) {
        _selectedMentions.value = _selectedMentions.value.filter { it.userId != userId }
    }

    fun clearMentions() {
        _selectedMentions.value = emptyList()
    }

    /*
        피드백 CRUD
     */

    fun loadFeedbacks() {
        viewModelScope.launch {
            _feedbackState.value = UiState.Loading

            when (val result = feedbackRepository.getFeedbacks(videoId)) {
                is DataResult.Success -> {
                    _feedbackState.value = UiState.Success(System.currentTimeMillis())
                    feedbacks.value = result.data
                    _feedbackItem.value = result.data.map { it.toUiItem() }
                }

                is DataResult.Error -> {
                    _feedbackState.value = UiState.Error(result.throwable.message, result.throwable)
                }
            }
        }
    }

    fun filterFeedback(checked: Boolean) {
        val currentUid = uid ?: return
        val data = feedbacks.value

        val filtered = if (checked) {
            data.filter { feedback ->
                feedback.taggedUsers.any { it.userId == currentUid }
            }
        } else {
            data
        }
        _feedbackItem.value = filtered.map { it.toUiItem() }
    }

    fun uploadFeedback(
        content: String,
        startTime: Double,
        endTime: Double? = null,
        imgUrl: String? = null
    ) {
        val currentUser = authRepository.getCurrentUser() ?: return
        val tempId = "temp_${System.currentTimeMillis()}"

        val taggedUsers = _selectedMentions.value
        val taggedUserIds = taggedUsers.mapNotNull { it.userId }

        val tempItem = FeedbackItem(
            feedbackId = tempId,
            videoId = videoId,
            author = FeedbackUser(
                userId = currentUser.uid,
                name = currentUser.displayName ?: "나"
            ),
            taggedUsers = taggedUsers,
            content = content,
            startTime = startTime,
            endTime = endTime,
            imgUrl = imgUrl,
            teamspaceId = teamspaceId,
            replyCount = 0,
            updatedAt = java.time.LocalDateTime.now(),
            status = CommentStatus.LOADING
        )

        _feedbackItem.value = listOf(tempItem) + _feedbackItem.value

        viewModelScope.launch {
            when (feedbackRepository.uploadFeedback(
                videoId = videoId,
                authorId = currentUser.uid,
                taggedUserIds = taggedUserIds,
                content = content,
                startTime = startTime,
                endTime = endTime,
                teamspaceId = teamspaceId,
                imageUrl = imgUrl
            )) {
                is DataResult.Success -> {
                    loadFeedbacks()
                }

                is DataResult.Error -> {
                    _feedbackItem.value = _feedbackItem.value.map { item ->
                        if (item.feedbackId == tempId) {
                            item.copy(status = CommentStatus.FAIL)
                        } else {
                            item
                        }
                    }
                }
            }
        }
    }

    fun retryUploadFeedback(feedbackItem: FeedbackItem) {
        _feedbackItem.value =
            _feedbackItem.value.filter { it.feedbackId != feedbackItem.feedbackId }

        uploadFeedback(
            content = feedbackItem.content,
            startTime = feedbackItem.startTime,
            endTime = feedbackItem.endTime,
            imgUrl = feedbackItem.imgUrl
        )
    }

    fun cancelFailedFeedback(feedbackId: String) {
        _feedbackItem.value = _feedbackItem.value.filter { it.feedbackId != feedbackId }
    }

    fun editFeedback(feedbackId: String, newContent: String) {
        val tagged = selectedMentions.value.mapNotNull { it.userId }
        viewModelScope.launch {
            when (feedbackRepository.editFeedback(feedbackId, newContent, tagged)) {
                is DataResult.Success -> {
                    loadFeedbacks()
                }

                is DataResult.Error -> {
                }
            }
        }
    }

    fun deleteFeedback(feedbackId: String) {
        viewModelScope.launch {
            when (feedbackRepository.deleteFeedback(feedbackId)) {
                is DataResult.Success -> {
                    loadFeedbacks()
                }

                is DataResult.Error -> {
                }
            }
        }
    }

    fun reportFeedback(feedbackId: String) {
        viewModelScope.launch {
            when (feedbackRepository.reportFeedback(feedbackId)) {
                is DataResult.Success -> {
                }

                is DataResult.Error -> {
                }
            }
        }
    }

    fun isMyFeedback(feedback: FeedbackItem): Boolean {
        return feedback.author.userId == uid
    }

    companion object {
        const val ALL_MEMBER_ID = "ALL"
        private const val KEY_VIDEO_ID = "videoId"
    }
}
