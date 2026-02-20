package com.baek.diract.presentation.report

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baek.diract.R
import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.repository.MyPageRepository
import com.baek.diract.domain.repository.VideoRepository
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

@HiltViewModel
class ReportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val myPageRepository: MyPageRepository,
    private val videoRepository: VideoRepository
) : ViewModel() {

    val contentType: ReportType = ReportType.entries
        .first { it.type == savedStateHandle.get<String>(ARG_CONTENT_TYPE) }

    val targetId: String = savedStateHandle.get<String>(ARG_TARGET_ID)
        ?: throw IllegalArgumentException("targetId is required")

    val reportedId: String? = savedStateHandle.get<String>(ARG_REPORTED_ID)

    private val _reportState = MutableStateFlow<UiState<Unit>>(UiState.None)
    val reportState: StateFlow<UiState<Unit>> = _reportState.asStateFlow()

    private val _toastEvent = MutableSharedFlow<ToastEvent>()
    val toastEvent: SharedFlow<ToastEvent> = _toastEvent.asSharedFlow()

    fun submitReport(description: String) {
        viewModelScope.launch {
            _reportState.value = UiState.Loading

            var resolvedReportedId = reportedId
            if (contentType == ReportType.VIDEO && resolvedReportedId == null) {
                // 비디오 신고 시 업로더 ID를 video 정보에서 조회
                when (val videoResult = videoRepository.getVideo(targetId)) {
                    is DataResult.Success -> resolvedReportedId = videoResult.data.uploaderId
                    is DataResult.Error -> {
                        _reportState.value = UiState.Error(throwable = videoResult.throwable)
                        _toastEvent.emit(ToastEvent(R.string.report_failed, isErr = true))
                        return@launch
                    }
                }
            }

            val result = myPageRepository.report(
                type = null,
                reportContentType = contentType.type,
                description = description,
                reportedId = resolvedReportedId ?: return@launch,
                videoId = if (contentType == ReportType.VIDEO) targetId else null,
                feedbackId = if (contentType == ReportType.FEEDBACK) targetId else null,
                replyId = if (contentType == ReportType.REPLY) targetId else null,
            )
            when (result) {
                is DataResult.Success -> {
                    _reportState.value = UiState.Success(Unit)
                    _toastEvent.emit(ToastEvent(R.string.report_success, isErr = false))
                }

                is DataResult.Error -> {
                    _reportState.value = UiState.Error(throwable = result.throwable)
                    _toastEvent.emit(ToastEvent(R.string.report_failed, isErr = true))
                }
            }
        }
    }

    companion object {
        const val ARG_CONTENT_TYPE = "arg_content_type"
        const val ARG_TARGET_ID = "arg_target_id"
        const val ARG_REPORTED_ID = "arg_reported_id"
    }
}
