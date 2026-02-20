package com.baek.diract.domain.usecase

import android.net.Uri
import com.baek.diract.data.util.VideoCompressionException
import com.baek.diract.data.util.VideoCompressor
import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.repository.VideoRepository
import javax.inject.Inject

class UploadVideoUseCase @Inject constructor(
    private val videoCompressor: VideoCompressor,
    private val videoRepository: VideoRepository
) {
    // 업로드 상태
    sealed interface UploadState {
        data class Compressing(val progress: Int) : UploadState
        data class Uploading(val progress: Int) : UploadState
        data object Completed : UploadState
        data class Failed(
            val phase: FailPhase,
            val message: String?,
            val compressedUri: Uri? = null, // 업로드 실패 시 압축된 파일 Uri
            val thumbnailUri: Uri? = null, // 업로드 실패 시 썸네일 Uri
            val duration: Double? = null // 업로드 실패 시 영상 길이
        ) : UploadState
    }

    // 실패 단계 (EXCEEDED: 용량 초과로 재시도 불가)
    enum class FailPhase { COMPRESSION, UPLOAD, EXCEEDED }

    suspend operator fun invoke(
        videoUri: Uri,
        title: String,
        tracksId: String,
        sectionId: String,
        onStateChanged: (UploadState) -> Unit
    ): DataResult<Unit> {
        return try {
            // 1. 압축
            onStateChanged(UploadState.Compressing(0))

            val compressionResult = videoCompressor.compress(
                inputUri = videoUri,
                progressListener = object : VideoCompressor.ProgressListener {
                    override fun onProgress(progress: Int) {
                        onStateChanged(UploadState.Compressing(progress))
                    }
                }
            )

            // 2. 업로드
            onStateChanged(UploadState.Uploading(0))

            val result = videoRepository.addVideo(
                tracksId = tracksId,
                sectionId = sectionId,
                videoUri = compressionResult.compressedUri,
                thumbnailUri = compressionResult.thumbnailUri,
                title = title,
                duration = compressionResult.durationSeconds,
                onProgress = { progress ->
                    onStateChanged(UploadState.Uploading(progress))
                }
            )

            // 3. 결과 처리
            when (result) {
                is DataResult.Success -> {
                    // 업로드 성공 시 압축 파일 정리
                    videoCompressor.cleanupCompressedFile(compressionResult.compressedUri)
                    onStateChanged(UploadState.Completed)
                    DataResult.Success(Unit)
                }

                is DataResult.Error -> {
                    onStateChanged(
                        UploadState.Failed(
                            phase = FailPhase.UPLOAD,
                            message = result.throwable.message,
                            compressedUri = compressionResult.compressedUri,
                            thumbnailUri = compressionResult.thumbnailUri,
                            duration = compressionResult.durationSeconds
                        )
                    )
                    result
                }
            }
        } catch (e: VideoCompressionException.FileTooLarge) {
            // 용량 초과는 재시도 불가
            onStateChanged(UploadState.Failed(FailPhase.EXCEEDED, e.message))
            DataResult.Error(e)
        } catch (e: VideoCompressionException.CompressionFailed) {
            // 압축 실패
            onStateChanged(UploadState.Failed(FailPhase.COMPRESSION, e.message))
            DataResult.Error(e)
        } catch (e: Exception) {
            onStateChanged(UploadState.Failed(FailPhase.COMPRESSION, e.message))
            DataResult.Error(e)
        }
    }

    // 업로드만 재시도 (압축 성공 후 업로드 실패 시 사용)
    suspend fun uploadOnly(
        compressedUri: Uri,
        thumbnailUri: Uri,
        duration: Double,
        title: String,
        tracksId: String,
        sectionId: String,
        onStateChanged: (UploadState) -> Unit
    ): DataResult<Unit> {
        return try {
            onStateChanged(UploadState.Uploading(0))

            val result = videoRepository.addVideo(
                tracksId = tracksId,
                sectionId = sectionId,
                videoUri = compressedUri,
                thumbnailUri = thumbnailUri,
                title = title,
                duration = duration,
                onProgress = { progress ->
                    onStateChanged(UploadState.Uploading(progress))
                }
            )

            when (result) {
                is DataResult.Success -> {
                    // 업로드 성공 시 압축 파일 정리
                    videoCompressor.cleanupCompressedFile(compressedUri)
                    onStateChanged(UploadState.Completed)
                    DataResult.Success(Unit)
                }

                is DataResult.Error -> {
                    onStateChanged(
                        UploadState.Failed(
                            phase = FailPhase.UPLOAD,
                            message = result.throwable.message,
                            compressedUri = compressedUri,
                            thumbnailUri = thumbnailUri,
                            duration = duration
                        )
                    )
                    result
                }
            }
        } catch (e: Exception) {
            onStateChanged(
                UploadState.Failed(
                    phase = FailPhase.UPLOAD,
                    message = e.message,
                    compressedUri = compressedUri,
                    thumbnailUri = thumbnailUri,
                    duration = duration
                )
            )
            DataResult.Error(e)
        }
    }
}
