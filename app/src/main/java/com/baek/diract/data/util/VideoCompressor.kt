package com.baek.diract.data.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.baek.diract.domain.model.CompressionResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/*
    비디오 압축 유틸리티 클래스
        - 720p 화질이 넘는다면 압축
        - 720p 이하인데 지정한 용량(maxOriginalFileSizeMB)이 넘으면 업로드 차단
        - 압축 후에도 지정한 용량(maxCompressedFileSizeMB)이 넘으면 업로드 차단
 */
@Singleton
class VideoCompressor @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "VideoCompressor"

        // 해상도 제한 (720p)
        private const val MAX_RESOLUTION = 1280

        // 용량 제한 (MB)
        private const val TARGET_FILE_SIZE_MB = 50.0       // 목표 용량
        private const val MAX_ORIGINAL_FILE_SIZE_MB = 500.0 // 원본 최대 허용 용량
        private const val MAX_COMPRESSED_FILE_SIZE_MB = 80.0 // 압축 후 최대 허용 용량
    }

    // 압축 진행률 콜백
    interface ProgressListener {
        fun onProgress(progress: Int)
    }

    // 비디오 압축이 필요한지 검사하고 필요시 압축
    suspend fun compress(
        inputUri: Uri,
        progressListener: ProgressListener? = null
    ): CompressionResult = withContext(Dispatchers.IO) {
        val resultUri = compressIfNeeded(inputUri, progressListener)
        val durationSeconds = getVideoDuration(resultUri)
        val thumbnailUri = extractThumbnail(resultUri)

        CompressionResult(
            compressedUri = resultUri,
            durationSeconds = durationSeconds,
            thumbnailUri = thumbnailUri
        )
    }

    // 압축 필요 여부 판단 후 압축 진행
    private suspend fun compressIfNeeded(
        inputUri: Uri,
        progressListener: ProgressListener?
    ): Uri {
        // 1. 해상도 체크
        val (width, height) = getVideoResolution(inputUri)
        Log.d(TAG, "원본해상도: ${width}x${height}")

        // 2. 원본 파일 크기 체크
        val fileSizeMB = getFileSizeMB(inputUri)
        Log.d(TAG, "원본 용량: ${"%.2f".format(fileSizeMB)}MB")

        // 안전장치 1: 원본이 너무 크면 압축 시도조차 하지 않음
        if (fileSizeMB > MAX_ORIGINAL_FILE_SIZE_MB) {
            Log.e(TAG, "원본 용량 초과: ${"%.2f".format(fileSizeMB)}MB > ${MAX_ORIGINAL_FILE_SIZE_MB.toInt()}MB")
            throw VideoCompressionException.FileTooLarge("원본 파일이 너무 큽니다. (${MAX_ORIGINAL_FILE_SIZE_MB.toInt()}MB 초과)")
        }

        // 3. 압축 필요 여부 판단
        val maxDimension = maxOf(width, height)

        // 720p 이하이고 용량도 목표치 이하면 압축 스킵
        if (maxDimension <= MAX_RESOLUTION && fileSizeMB <= TARGET_FILE_SIZE_MB) {
            Log.d(TAG, "압축 스킵: 720p 이하 + ${TARGET_FILE_SIZE_MB.toInt()}MB 이하")
            return inputUri
        }

        // 4. 압축 필요 (해상도가 크거나 용량이 큼)
        if (maxDimension > MAX_RESOLUTION) {
            Log.d(TAG, "해상도 압축 시작: ${width}x${height} → 720p")
        } else {
            Log.d(TAG, "용량 압축 시작: ${"%.2f".format(fileSizeMB)}MB → ${TARGET_FILE_SIZE_MB.toInt()}MB")
        }

        return compressVideo(inputUri, progressListener)
    }

    // 비디오 압축 실행 (Transformer는 메인 스레드에서 실행)
    @androidx.annotation.OptIn(UnstableApi::class)
    private suspend fun compressVideo(
        inputUri: Uri,
        progressListener: ProgressListener?
    ): Uri = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val outputFile = createVideoOutputFile()
            val outputPath = outputFile.absolutePath
            Log.d(TAG, "압축 출력 경로: $outputPath")

            // 720p로 해상도 제한 (높이 기준, 비율 유지)
            val presentation = Presentation.createForHeight(720)
            val effects = Effects(emptyList(), listOf(presentation))

            val editedMediaItem = EditedMediaItem.Builder(MediaItem.fromUri(inputUri))
                .setEffects(effects)
                .build()

            val transformer = Transformer.Builder(context)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        // 압축 완료 후 용량 검증
                        val compressedSizeMB = outputFile.length().toDouble() / (1024 * 1024)
                        Log.d(TAG, "압축 완료: ${"%.2f".format(compressedSizeMB)}MB")

                        if (compressedSizeMB > MAX_COMPRESSED_FILE_SIZE_MB) {
                            Log.e(TAG, "압축 후 용량 초과: ${"%.2f".format(compressedSizeMB)}MB > ${MAX_COMPRESSED_FILE_SIZE_MB.toInt()}MB")
                            outputFile.delete()
                            continuation.resumeWithException(
                                VideoCompressionException.FileTooLarge("압축 후에도 용량이 너무 큽니다. (${MAX_COMPRESSED_FILE_SIZE_MB.toInt()}MB 초과)")
                            )
                        } else {
                            Log.d(TAG, "최종 통과: ${"%.2f".format(compressedSizeMB)}MB ≤ ${MAX_COMPRESSED_FILE_SIZE_MB.toInt()}MB")
                            continuation.resume(Uri.fromFile(outputFile))
                        }
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        Log.e(TAG, "압축 실패", exportException)
                        outputFile.delete()
                        continuation.resumeWithException(
                            VideoCompressionException.CompressionFailed(
                                exportException.message ?: "비디오 압축에 실패했습니다."
                            )
                        )
                    }
                })
                .build()

            transformer.start(editedMediaItem, outputPath)
            Log.d(TAG, "Transformer 시작됨")

            // 진행률 업데이트를 위한 폴링
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            val progressHolder = ProgressHolder()

            val progressRunnable = object : Runnable {
                override fun run() {
                    if (!continuation.isCompleted) {
                        val state = transformer.getProgress(progressHolder)
                        if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                            progressListener?.onProgress(progressHolder.progress)
                        }
                        handler.postDelayed(this, 100)
                    }
                }
            }
            handler.post(progressRunnable)

            continuation.invokeOnCancellation {
                handler.removeCallbacks(progressRunnable)
                transformer.cancel()
                outputFile.delete()
            }
        }
    }

    // 비디오 해상도 조회
    private fun getVideoResolution(uri: Uri): Pair<Int, Int> {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            Pair(width, height)
        } catch (e: Exception) {
            Log.e(TAG, "해상도 조회 실패", e)
            Pair(0, 0)
        } finally {
            retriever.release()
        }
    }

    // 파일 크기 조회 (MB)
    private fun getFileSizeMB(uri: Uri): Double {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                pfd.statSize.toDouble() / (1024 * 1024)
            } ?: 0.0
        } catch (e: Exception) {
            Log.e(TAG, "파일 크기 조회 실패", e)
            0.0
        }
    }

    // 비디오에서 썸네일 추출 (1초 지점)
    suspend fun extractThumbnail(videoUri: Uri): Uri = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)

            // 1초 지점에서 프레임 추출 (마이크로초 단위)
            val bitmap = retriever.getFrameAtTime(
                1_000_000L,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            ) ?: retriever.getFrameAtTime(0L) // 1초가 없으면 첫 프레임

            requireNotNull(bitmap) { "Failed to extract thumbnail from video" }

            val thumbnailFile = createThumbnailOutputFile()
            FileOutputStream(thumbnailFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            }
            bitmap.recycle()

            Uri.fromFile(thumbnailFile)
        } finally {
            retriever.release()
        }
    }

    // 비디오 길이 조회 (초 단위)
    fun getVideoDuration(uri: Uri): Double {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L
            durationMs / 1000.0
        } catch (e: Exception) {
            0.0
        } finally {
            retriever.release()
        }
    }

    private fun createVideoOutputFile(): File {
        val cacheDir = File(context.cacheDir, "compressed_videos")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        return File(cacheDir, "compressed_${System.currentTimeMillis()}.mp4")
    }

    private fun createThumbnailOutputFile(): File {
        val cacheDir = File(context.cacheDir, "thumbnails")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        return File(cacheDir, "thumbnail_${System.currentTimeMillis()}.jpg")
    }

    // 압축 파일 정리 (업로드 성공/실패/취소 시 호출)
    fun cleanupCompressedFile(uri: Uri) {
        try {
            if (uri.path?.contains("compressed_") == true) {
                val file = File(uri.path!!)
                if (file.exists()) {
                    file.delete()
                    Log.d(TAG, "압축 파일 정리: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "압축 파일 정리 실패", e)
        }
    }
}

// 비디오 압축 관련 예외
sealed class VideoCompressionException(message: String) : Exception(message) {
    class FileTooLarge(message: String) : VideoCompressionException(message)
    class CompressionFailed(message: String) : VideoCompressionException(message)
}
