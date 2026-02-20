package com.baek.diract.data.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.baek.diract.di.DownloadClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/*
    비디오 캐시 관리
    - 캐시된 비디오 확인
    - API URL로 다운로드 후 캐시 저장
 */
@Singleton
class VideoCacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @DownloadClient private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "VideoCacheManager"
        private const val CACHE_DIR_NAME = "video_cache"
    }

    // 캐시 디렉토리
    private val cacheDir: File
        get() = File(context.cacheDir, CACHE_DIR_NAME).also {
            if (!it.exists()) it.mkdirs()
        }

    // 캐시된 비디오 파일 경로
    private fun getCacheFile(videoId: String): File {
        return File(cacheDir, "$videoId.mp4")
    }

    // 캐시 확인
    fun isCached(videoId: String): Boolean {
        return getCacheFile(videoId).exists()
    }

    // 캐시된 비디오 Uri 반환
    fun getCachedUri(videoId: String): Uri? {
        val file = getCacheFile(videoId)
        return if (file.exists()) Uri.fromFile(file) else null
    }

    // URL에서 다운로드 후 캐시 저장
    suspend fun downloadAndCache(
        videoId: String,
        videoUrl: String,
        onProgress: ((Int) -> Unit)? = null
    ): Uri = withContext(Dispatchers.IO) {
        val cacheFile = getCacheFile(videoId)

        if (cacheFile.exists()) {
            Log.d(TAG, "캐시에서 로드: $videoId")
            return@withContext Uri.fromFile(cacheFile)
        }

        Log.d(TAG, "다운로드 시작: $videoId")

        val request = Request.Builder().url(videoUrl).get().build()
        val response = okHttpClient.newCall(request).execute()

        if (!response.isSuccessful) throw Exception("다운로드 실패: ${response.code}")

        val body = response.body ?: throw Exception("응답 본문이 없습니다")
        val contentLength = body.contentLength()

        body.byteStream().use { input ->
            cacheFile.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var downloaded = 0L
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    downloaded += read
                    if (contentLength > 0) {
                        onProgress?.invoke((downloaded * 100 / contentLength).toInt())
                    }
                }
            }
        }

        Log.d(TAG, "다운로드 완료: $videoId (${cacheFile.length() / 1024}KB)")
        onProgress?.invoke(100)

        Uri.fromFile(cacheFile)
    }

    // 특정 비디오 캐시 삭제
    fun deleteCache(videoId: String) {
        val file = getCacheFile(videoId)
        if (file.exists()) {
            file.delete()
            Log.d(TAG, "캐시 삭제: $videoId")
        }
    }

    // 전체 캐시 삭제
    fun clearAllCache() {
        cacheDir.listFiles()?.forEach { it.delete() }
        Log.d(TAG, "전체 캐시 삭제 완료")
    }

    // 캐시 크기 조회 (바이트)
    fun getCacheSize(): Long {
        return cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
    }
}