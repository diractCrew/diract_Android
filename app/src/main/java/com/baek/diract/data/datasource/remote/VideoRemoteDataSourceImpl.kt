package com.baek.diract.data.datasource.remote

import android.net.Uri
import android.util.Log
import com.baek.diract.data.dto.SectionDto
import com.baek.diract.data.dto.VideoDto
import com.baek.diract.data.dto.VideoWithTrackDto
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class VideoRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : VideoRemoteDataSource {

    override suspend fun getSections(tracksId: String): List<SectionDto> {
        val snapshot = firestore
            .collection("tracks")
            .document(tracksId)
            .collection("section")
            .orderBy("created_at")
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(SectionDto::class.java)
        }
    }

    override suspend fun getVideos(tracksId: String, sectionId: String): List<VideoWithTrackDto> {
        val videoIds = mutableListOf<Pair<String, String>>() // Pair(trackId, videoId)

        val trackSnapshot = firestore
            .collection("tracks")
            .document(tracksId)
            .collection("section")
            .document(sectionId)
            .collection("track")
            .get()
            .await()

        trackSnapshot.documents.forEach { doc ->
            val videoId = doc.getString("video_id")
            if (videoId != null) {
                videoIds.add(Pair(doc.id, videoId))
            }
        }

        // video 컬렉션에서 비디오 정보 조회
        val videos = videoIds.mapNotNull { (trackId, videoId) ->
            val videoDoc = firestore
                .collection("video")
                .document(videoId)
                .get()
                .await()

            videoDoc.toObject(VideoDto::class.java)?.let { dto ->
                VideoWithTrackDto(
                    trackId = trackId,
                    video = dto
                )
            }
        }
        return videos.sortedByDescending { it.video.created_at }
    }

    override suspend fun addVideo(
        tracksId: String,
        sectionId: String,
        videoUri: Uri,
        thumbnailUri: Uri,
        title: String,
        duration: Double,
        uploaderId: String,
        onProgress: ((Int) -> Unit)?
    ) {
        // 1. video 컬렉션에 새 문서 생성
        val videoRef = firestore.collection("video").document()
        val videoId = videoRef.id
        val now = Timestamp.now()

        // 2. 비디오 업로드 (진행률: 0~90%)
        val videoStorageRef = storage.reference.child("video/$videoId/$videoId.video.mov")
        val videoUploadTask = videoStorageRef.putFile(videoUri)

        if (onProgress != null) {
            videoUploadTask.addOnProgressListener { snapshot ->
                val videoProgress =
                    ((snapshot.bytesTransferred * 90) / snapshot.totalByteCount).toInt()
                onProgress(videoProgress)
            }
        }

        videoUploadTask.await()
        val videoUrl = videoStorageRef.downloadUrl.await().toString()

        // 3. Custom bucket Storage에 썸네일 업로드 (진행률: 90~100%)
        val thumbnailStorageRef = storage.reference.child("video/$videoId/$videoId.jpg")
        val thumbnailUploadTask = thumbnailStorageRef.putFile(thumbnailUri)

        if (onProgress != null) {
            thumbnailUploadTask.addOnProgressListener { snapshot ->
                val thumbnailProgress =
                    90 + ((snapshot.bytesTransferred * 10) / snapshot.totalByteCount).toInt()
                onProgress(thumbnailProgress)
            }
        }

        thumbnailUploadTask.await()
        val thumbnailUrl = thumbnailStorageRef.downloadUrl.await().toString()

        // 4. video 문서 저장
        val videoData = hashMapOf(
            "video_id" to videoId,
            "video_title" to title,
            "video_duration" to duration,
            "video_url" to videoUrl,
            "thumbnail_url" to thumbnailUrl,
            "uploader_id" to uploaderId,
            "created_at" to now,
            "updated_at" to now
        )
        videoRef.set(videoData).await()

        // 5. track 서브컬렉션에 참조 추가
        val trackRef = firestore
            .collection("tracks")
            .document(tracksId)
            .collection("section")
            .document(sectionId)
            .collection("track")
            .document()

        trackRef.set(mapOf("video_id" to videoId)).await()

        // 완료 시 100% 콜백
        onProgress?.invoke(100)
    }

    override suspend fun editVideoTitle(videoId: String, editedTitle: String) {
        firestore
            .collection("video")
            .document(videoId)
            .update(
                mapOf(
                    "video_title" to editedTitle,
                    "updated_at" to Timestamp.now()
                )
            )
            .await()
    }

    override suspend fun moveVideoSection(
        tracksId: String,
        fromSectionId: String,
        toSectionId: String,
        trackId: String
    ) {
        val fromTrackRef = firestore
            .collection("tracks")
            .document(tracksId)
            .collection("section")
            .document(fromSectionId)
            .collection("track")
            .document(trackId)

        // 기존 track 문서 데이터 가져오기
        val trackDoc = fromTrackRef.get().await()

        // 새 섹션에 track 추가
        val toTrackRef = firestore
            .collection("tracks")
            .document(tracksId)
            .collection("section")
            .document(toSectionId)
            .collection("track")
            .document()

        val newTrackData = hashMapOf(
            "video_id" to trackDoc.getString("video_id"),
            "track_id" to toTrackRef.id,
            "created_at" to trackDoc.getTimestamp("created_at"),
            "updated_at" to Timestamp.now(),
            "section_id" to toSectionId
        )
        toTrackRef.set(newTrackData).await()

        // 기존 track 삭제
        fromTrackRef.delete().await()
    }

    override suspend fun deleteVideo(
        tracksId: String,
        sectionId: String,
        trackId: String,
        videoId: String
    ) {
        // 1. track 서브컬렉션에서 참조 삭제
        firestore
            .collection("tracks")
            .document(tracksId)
            .collection("section")
            .document(sectionId)
            .collection("track")
            .document(trackId)
            .delete()
            .await()

        // 2. Custom bucket Storage에서 비디오/썸네일 파일 삭제
        val folderRef = storage.reference.child("video/$videoId")
        try {
            val listResult = folderRef.listAll().await()
            listResult.items.forEach { fileRef ->
                fileRef.delete().await()
            }
//            storage.reference.child("video/$videoId/$videoId.mov").delete().await()
//            storage.reference.child("video/$videoId/$videoId.jpg").delete().await()
        } catch (e: Exception) {
            Log.e("deleteVideoFolder", "폴더 삭제 실패: video/$videoId", e)
        }

        // 3. video 컬렉션에서 문서 삭제
        firestore
            .collection("video")
            .document(videoId)
            .delete()
            .await()
    }

    override suspend fun addSection(tracksId: String, title: String): SectionDto {
        val sectionRef = firestore
            .collection("tracks")
            .document(tracksId)
            .collection("section")
            .document()

        val sectionId = sectionRef.id
        val now = Timestamp.now()
        val sectionData = hashMapOf(
            "section_id" to sectionId,
            "section_title" to title,
            "created_at" to now,
            "updated_at" to now
        )

        sectionRef.set(sectionData).await()

        return SectionDto(
            section_id = sectionRef.id,
            section_title = title,
            created_at = now,
            updated_at = now
        )
    }

    override suspend fun editSection(
        tracksId: String,
        sectionId: String,
        title: String
    ): SectionDto {
        val sectionRef = firestore
            .collection("tracks")
            .document(tracksId)
            .collection("section")
            .document(sectionId)

        val now = Timestamp.now()
        sectionRef.update(
            mapOf(
                "section_title" to title,
                "updated_at" to now
            )
        ).await()

        val updatedDoc = sectionRef.get().await()
        return updatedDoc.toObject(SectionDto::class.java)?.copy(section_id = sectionId)
            ?: throw IllegalStateException("Section not found")
    }

    override suspend fun deleteSection(tracksId: String, sectionId: String) {
        coroutineScope {
            val sectionRef = firestore
                .collection("tracks")
                .document(tracksId)
                .collection("section")
                .document(sectionId)

            val trackRef = sectionRef.collection("track")

            val trackSnapshots = trackRef.get().await()

            val videoIds = trackSnapshots.documents
                .mapNotNull { it.getString("video_id") }
                .distinct()

            //1. video+storage 삭제
            videoIds.map { videoId ->
                async {
                    // video 문서 삭제
                    firestore.collection("video")
                        .document(videoId)
                        .delete()
                        .await()

                    // storage 파일 전부 삭제
                    val videoFolderRef = storage.reference.child("video/$videoId")
                    val listResult = videoFolderRef.listAll().await()

                    listResult.items.map { item ->
                        async {
                            item.delete().await()
                        }
                    }.awaitAll()
                }
            }.awaitAll()

            //2. track삭제
            trackSnapshots.documents.map { doc ->
                async {
                    doc.reference.delete().await()
                }
            }.awaitAll()

            // 섹션 삭제
            sectionRef.delete().await()
        }
    }
}
