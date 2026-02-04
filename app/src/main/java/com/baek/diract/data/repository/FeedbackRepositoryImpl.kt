package com.baek.diract.data.repository

import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.model.FeedbackUser
import com.baek.diract.domain.model.Feedback
import com.baek.diract.domain.model.Reply
import com.baek.diract.domain.repository.FeedbackRepository
import java.time.LocalDateTime
import javax.inject.Inject

class FeedbackRepositoryImpl @Inject constructor() : FeedbackRepository {

    override suspend fun getTeamspaceUsers(teamspaceId: String): DataResult<List<FeedbackUser>> {
        return DataResult.Success(mockUsers)
    }

    override suspend fun getFeedbacks(videoId: String): DataResult<List<Feedback>> {
        val feedbacks = mockFeedbacks.filter { it.videoId == videoId }
        return DataResult.Success(feedbacks)
    }

    override suspend fun uploadFeedback(
        videoId: String,
        authorId: String,
        taggedUserIds: List<String>,
        content: String,
        startTime: Double,
        endTime: Double?,
        teamspaceId: String,
        imageUrl: String?
    ): DataResult<Unit> {
        val idx = mockFeedbacks.size
        mockFeedbacks.add(
            Feedback(
                feedbackId = "feedback${idx + 2}",
                videoId = videoId,
                author = FeedbackUser(authorId,"name"),
                taggedUsers = taggedUserIds.map { FeedbackUser(it,"name") },
                content = "이 부분에서 동작이 조금 어색해 보입니다. 좀 더 자연스럽게 수정해주세요.",
                startTime = startTime,
                endTime = endTime,
                imgUrl = imageUrl,
                teamspaceId = teamspaceId,
                updatedAt = LocalDateTime.now().minusHours(2)
            )
        )
        return DataResult.Success(Unit)
    }

    override suspend fun editFeedback(feedbackId: String, newContent: String): DataResult<Unit> {
        val index = mockFeedbacks.indexOfFirst { it.feedbackId == feedbackId }
        if (index != -1) {
            mockFeedbacks[index] = mockFeedbacks[index].copy(
                content = newContent,
                updatedAt = LocalDateTime.now()
            )
            return DataResult.Success(Unit)
        }
        return DataResult.Error(IllegalArgumentException("Feedback not found"))
    }

    override suspend fun deleteFeedback(feedbackId: String): DataResult<Unit> {
        val removed = mockFeedbacks.removeIf { it.feedbackId == feedbackId }
        return if (removed) {
            // 해당 피드백의 댓글도 삭제
            mockReplies.removeIf { it.feedbackId == feedbackId }
            DataResult.Success(Unit)
        } else {
            DataResult.Error(IllegalArgumentException("Feedback not found"))
        }
    }

    override suspend fun reportFeedback(feedbackId: String): DataResult<Unit> {
        // Mock: 신고 성공으로 처리
        return DataResult.Success(Unit)
    }

    override suspend fun getReplies(feedbackId: String): DataResult<List<Reply>> {
        val replies = mockReplies.filter { it.feedbackId == feedbackId }
        return DataResult.Success(replies)
    }

    override suspend fun uploadReply(reply: Reply): DataResult<Unit> {
        mockReplies.add(reply)
        // 해당 피드백의 replyCount 증가
        val feedbackIndex = mockFeedbacks.indexOfFirst { it.feedbackId == reply.feedbackId }
        if (feedbackIndex != -1) {
            mockFeedbacks[feedbackIndex] = mockFeedbacks[feedbackIndex].copy(
                replyCount = mockFeedbacks[feedbackIndex].replyCount + 1
            )
        }
        return DataResult.Success(Unit)
    }

    override suspend fun editReply(replyId: String, newContent: String): DataResult<Unit> {
        val index = mockReplies.indexOfFirst { it.replyId == replyId }
        if (index != -1) {
            mockReplies[index] = mockReplies[index].copy(
                content = newContent,
                updatedAt = LocalDateTime.now()
            )
            return DataResult.Success(Unit)
        }
        return DataResult.Error(IllegalArgumentException("Reply not found"))
    }

    override suspend fun deleteReply(replyId: String): DataResult<Unit> {
        val reply = mockReplies.find { it.replyId == replyId }
        if (reply != null) {
            mockReplies.remove(reply)
            // 해당 피드백의 replyCount 감소
            val feedbackIndex = mockFeedbacks.indexOfFirst { it.feedbackId == reply.feedbackId }
            if (feedbackIndex != -1) {
                mockFeedbacks[feedbackIndex] = mockFeedbacks[feedbackIndex].copy(
                    replyCount = (mockFeedbacks[feedbackIndex].replyCount - 1).coerceAtLeast(0)
                )
            }
            return DataResult.Success(Unit)
        }
        return DataResult.Error(IllegalArgumentException("Reply not found"))
    }

    override suspend fun reportReply(replyId: String): DataResult<Unit> {
        // Mock: 신고 성공으로 처리
        return DataResult.Success(Unit)
    }

    // Mock 데이터
    private val mockUsers = listOf(
        FeedbackUser(userId = "user1", name = "김철수"),
        FeedbackUser(userId = "user2", name = "이영희"),
        FeedbackUser(userId = "user3", name = "박민수"),
        FeedbackUser(userId = "user4", name = "최지원"),
        FeedbackUser(userId = "user5", name = "정수진")
    )

    private val mockFeedbacks = mutableListOf(
        Feedback(
            feedbackId = "feedback1",
            videoId = "video1",
            author = mockUsers[0],
            taggedUsers = listOf(mockUsers[1]),
            content = "이 부분에서 동작이 조금 어색해 보입니다. 좀 더 자연스럽게 수정해주세요.",
            startTime = 10.5,
            endTime = 15.0,
            imgUrl = null,
            teamspaceId = "team1",
            replyCount = 2,
            updatedAt = LocalDateTime.now().minusHours(2)
        ),
        Feedback(
            feedbackId = "feedback2",
            videoId = "video1",
            author = mockUsers[1],
            taggedUsers = emptyList(),
            content = "여기 타이밍이 잘 맞았네요! 좋습니다.",
            startTime = 25.0,
            endTime = null,
            imgUrl = null,
            teamspaceId = "team1",
            replyCount = 0,
            updatedAt = LocalDateTime.now().minusHours(1)
        ),
        Feedback(
            feedbackId = "feedback3",
            videoId = "video1",
            author = mockUsers[2],
            taggedUsers = listOf(mockUsers[0], mockUsers[1]),
            content = "이 구간 전체적으로 템포가 느린 것 같아요. 확인 부탁드립니다.",
            startTime = 45.0,
            endTime = 60.0,
            imgUrl = null,
            teamspaceId = "team1",
            replyCount = 3,
            updatedAt = LocalDateTime.now().minusMinutes(30)
        )
    )

    private val mockReplies = mutableListOf(
        Reply(
            replyId = "reply1",
            feedbackId = "feedback1",
            author = mockUsers[1],
            taggedUsers = listOf(mockUsers[0]),
            content = "네, 수정하겠습니다!",
            updatedAt = LocalDateTime.now().minusHours(1)
        ),
        Reply(
            replyId = "reply2",
            feedbackId = "feedback1",
            author = mockUsers[0],
            taggedUsers = emptyList(),
            content = "감사합니다. 확인했어요.",
            updatedAt = LocalDateTime.now().minusMinutes(45)
        ),
        Reply(
            replyId = "reply3",
            feedbackId = "feedback3",
            author = mockUsers[0],
            taggedUsers = listOf(mockUsers[2]),
            content = "어느 정도 템포로 수정하면 될까요?",
            updatedAt = LocalDateTime.now().minusMinutes(25)
        ),
        Reply(
            replyId = "reply4",
            feedbackId = "feedback3",
            author = mockUsers[2],
            taggedUsers = listOf(mockUsers[0]),
            content = "1.2배속 정도면 좋을 것 같아요.",
            updatedAt = LocalDateTime.now().minusMinutes(20)
        ),
        Reply(
            replyId = "reply5",
            feedbackId = "feedback3",
            author = mockUsers[0],
            taggedUsers = emptyList(),
            content = "알겠습니다. 수정할게요!",
            updatedAt = LocalDateTime.now().minusMinutes(15)
        )
    )
}
