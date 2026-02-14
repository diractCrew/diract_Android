package com.baek.diract.presentation.report

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baek.diract.R
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
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val contentType: ReportType = ReportType.entries
        .first { it.type == savedStateHandle.get<String>(ARG_CONTENT_TYPE) }

    val targetId: String = savedStateHandle.get<String>(ARG_TARGET_ID)
        ?: throw IllegalArgumentException("targetId is required")

    val reportedId: String = savedStateHandle.get<String>(ARG_REPORTED_ID)
        ?: throw IllegalArgumentException("reportedId is required")

    private val _reportState = MutableStateFlow<UiState<Unit>>(UiState.None)
    val reportState: StateFlow<UiState<Unit>> = _reportState.asStateFlow()

    private val _toastEvent = MutableSharedFlow<ToastEvent>()
    val toastEvent: SharedFlow<ToastEvent> = _toastEvent.asSharedFlow()

    fun submitReport(description: String) {
        viewModelScope.launch {
            _reportState.value = UiState.Loading
            // TODO: Repository를 통해 신고 API 호출
            _reportState.value = UiState.Success(Unit)
            _toastEvent.emit(ToastEvent(R.string.report_success, isErr = false))
        }
    }

    companion object {
        const val ARG_CONTENT_TYPE = "arg_content_type"
        const val ARG_TARGET_ID = "arg_target_id"
        const val ARG_REPORTED_ID = "arg_reported_id"
    }
}
