package com.baek.diract.domain.repository

import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.model.TeamMemberSummary
import com.baek.diract.domain.model.TeamspaceDetail
import com.baek.diract.domain.model.TeamspaceSummary

interface TeamspaceRepository {

    /** 홈 분기: 내가 속한 팀스페이스 목록(없으면 empty) */
    suspend fun getMyTeamspaces(): DataResult<List<TeamspaceSummary>>

    suspend fun getTeamspaceDetail(teamspaceId: String): DataResult<TeamspaceDetail>

    /** 팀스페이스 생성 (생성자는 ownerId=현재 팀장) */
    suspend fun createTeamspace(name: String): DataResult<TeamspaceSummary>

    /** 이름 수정 (팀장만) */
    suspend fun renameTeamspace(teamspaceId: String, newName: String): DataResult<Unit>

    /** 삭제 (팀장만) */
    suspend fun deleteTeamspace(teamspaceId: String): DataResult<Unit>

    /** 멤버 목록 */
    suspend fun getMembers(teamspaceId: String): DataResult<List<TeamMemberSummary>>

    /** 멤버 추가 */
    suspend fun addMember(teamspaceId: String,userId: String): DataResult<Unit>

    /** 멤버 삭제 */
    suspend fun deleteMember(teamspaceId: String,userId: String): DataResult<Unit>

    /** 팀장 위임: 성공하면 서버에서 ownerId가 newLeaderId로 바뀜 */
    suspend fun transferLeader(teamspaceId: String, newLeaderId: String): DataResult<Unit>

    /** 나가기 (팀장 정책은 서버가 결정: 보통 팀장은 위임 후 가능) */
    suspend fun leaveTeamspace(teamspaceId: String, userId: String): DataResult<Unit>

    /** 내보내기 (팀장만) */
    suspend fun kickMembers(teamspaceId: String, memberIds: List<String>): DataResult<Unit>
}
