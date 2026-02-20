package com.baek.diract.data.repository

import android.util.Log
import com.baek.diract.data.remote.api.CreateReportRequest
import com.baek.diract.data.remote.api.ReportApi
import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.repository.MyPageRepository
import retrofit2.HttpException
import javax.inject.Inject

class MyPageRepositoryImpl @Inject constructor(
    private val reportApi: ReportApi
) : MyPageRepository {

    override suspend fun report(
        type: String?,
        reportContentType: String,
        description: String,
        reportedId: String,
        videoId: String?,
        feedbackId: String?,
        replyId: String?
    ): DataResult<Unit> = safeCall {
        val request = CreateReportRequest(
            type = type ?: "other",
            reportContentType = reportContentType,
            description = description,
            reportedId = reportedId,
            videoId = videoId,
            feedbackId = feedbackId,
            replyId = replyId
        )
        val response = reportApi.createReport(request)
        if (!response.success) throw Exception(response.message ?: "신고 실패")
        Unit
    }

    private inline fun <T> safeCall(block: () -> T): DataResult<T> {
        return try {
            DataResult.Success(block())
        } catch (e: Exception) {
            val serverMessage = (e as? HttpException)
                ?.response()?.errorBody()?.string()
            Log.e(TAG, "serverMessage=$serverMessage", e)
            DataResult.Error(e)
        }
    }

    companion object {
        private const val TAG = "MyPageRepository"
    }
}
