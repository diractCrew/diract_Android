package com.baek.diract.data.repository

import com.baek.diract.data.local.UserPreferenceManager
import com.baek.diract.data.mapper.toDomain
import com.baek.diract.data.mapper.toTeamMemberSummary
import com.baek.diract.data.mapper.toTeamspaceSummary
import com.baek.diract.data.remote.api.AddMemberRequest
import com.baek.diract.data.remote.api.CreateTeamspaceRequest
import com.baek.diract.data.remote.api.TeamspaceApi
import com.baek.diract.data.remote.api.TransferOwnerRequest
import com.baek.diract.data.remote.api.UpdateTeamspaceRequest
import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.model.TeamMemberSummary
import com.baek.diract.domain.model.TeamspaceDetail
import com.baek.diract.domain.repository.TeamspaceRepository
import com.baek.diract.domain.model.TeamspaceSummary
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TeamspaceRepositoryImpl @Inject constructor(
    private val teamspaceApi: TeamspaceApi,
    private val userPreferenceManager: UserPreferenceManager
) : TeamspaceRepository {

    override val lastTeamspaceId: Flow<String?> = userPreferenceManager.lastTeamspaceId

    override suspend fun saveLastTeamspaceId(teamspaceId: String) {
        userPreferenceManager.saveLastTeamspaceId(teamspaceId)
    }

    override suspend fun clearLastTeamspaceId() {
        userPreferenceManager.clearLastTeamspace()
    }

    override suspend fun getMyTeamspaces(): DataResult<List<TeamspaceSummary>> {
        return try {
            val response = teamspaceApi.getMyTeamspaces()
            if (response.success && response.data != null) {
                DataResult.Success(response.data.map { it.toTeamspaceSummary() })
            } else {
                DataResult.Error(Exception(response.message ?: "팀스페이스 목록 조회 실패"))
            }
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    override suspend fun getTeamspaceDetail(teamspaceId: String): DataResult<TeamspaceDetail> {
        return try {
            val response = teamspaceApi.getTeamspaceDetail(teamspaceId)
            if (response.success && response.data != null) {
                DataResult.Success(response.data.toDomain())
            } else {
                DataResult.Error(Exception(response.message ?: "팀스페이스 상세 조회 실패"))
            }
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    override suspend fun createTeamspace(name: String): DataResult<TeamspaceSummary> {
        return try {
            val response = teamspaceApi.createTeamspace(
                CreateTeamspaceRequest(teamspaceName = name)
            )
            if (response.success && response.data != null) {
                DataResult.Success(response.data.toTeamspaceSummary())
            } else {
                DataResult.Error(Exception(response.message ?: "팀스페이스 생성 실패"))
            }
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    override suspend fun renameTeamspace(teamspaceId: String, newName: String): DataResult<Unit> {
        return try {
            val response = teamspaceApi.updateTeamspace(
                teamspaceId,
                UpdateTeamspaceRequest(teamspaceName = newName)
            )
            if (response.success) {
                DataResult.Success(Unit)
            } else {
                DataResult.Error(Exception(response.message ?: "팀스페이스 이름 변경 실패"))
            }
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    override suspend fun deleteTeamspace(teamspaceId: String): DataResult<Unit> {
        return try {
            val response = teamspaceApi.deleteTeamspace(teamspaceId)
            if (response.success) {
                DataResult.Success(Unit)
            } else {
                DataResult.Error(Exception(response.message ?: "팀스페이스 삭제 실패"))
            }
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    override suspend fun getMembers(teamspaceId: String): DataResult<List<TeamMemberSummary>> {
        return try {
            val response = teamspaceApi.getMembers(teamspaceId)
            if (response.success && response.data != null) {
                DataResult.Success(response.data.map { it.toTeamMemberSummary() })
            } else {
                DataResult.Error(Exception(response.message ?: "팀스페이스 멤버 조회 실패"))
            }
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }
    override suspend fun addMember(teamspaceId: String, userId: String): DataResult<Unit> {
        return try {
            val response = teamspaceApi.addMember(
                teamspaceId = teamspaceId,
                request = AddMemberRequest(userId = userId)
            )
            if (response.success) {
                DataResult.Success(Unit)
            } else {
                DataResult.Error(Exception(response.message ?: "멤버 추가 실패"))
            }
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    override suspend fun deleteMember(teamspaceId: String, userId: String): DataResult<Unit> {
        return try {
            val response = teamspaceApi.deleteMember(
                teamspaceId = teamspaceId,
                userId = userId
            )
            if (response.success) {
                DataResult.Success(Unit)
            } else {
                DataResult.Error(Exception(response.message ?: "멤버 삭제 실패"))
            }
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }
    override suspend fun transferLeader(teamspaceId: String, newLeaderId: String): DataResult<Unit> {
        return try {
            val res = teamspaceApi.transferOwnership(
                teamspaceId = teamspaceId,
                request = TransferOwnerRequest(newOwnerId = newLeaderId)
            )
            if (res.success) DataResult.Success(Unit)
            else DataResult.Error(IllegalStateException(res.message ?: "팀장 위임 실패"))
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }


    override suspend fun leaveTeamspace(teamspaceId: String, userId: String): DataResult<Unit> {
        android.util.Log.e("TeamspaceApi", "ENTER leaveTeamspace teamspaceId=$teamspaceId userId=$userId")
        return try {
            // ✅ userId 방어 (빈 값이면 바로 실패 처리)
            if (userId.isBlank()) {
                return DataResult.Error(IllegalStateException("userId is blank"))
            }

            val res = teamspaceApi.deleteMember(teamspaceId, userId)
            if (res.success) DataResult.Success(Unit)
            else DataResult.Error(IllegalStateException(res.message ?: "leaveTeamspace failed"))
        } catch (t: Throwable) {
            android.util.Log.e("TeamspaceApi", "leave fail teamspaceId=$teamspaceId userId=$userId", t)

            // ✅ Http 에러면 code/body까지 찍기
            if (t is retrofit2.HttpException) {
                android.util.Log.e(
                    "TeamspaceApi",
                    "code=${t.code()} body=${t.response()?.errorBody()?.string()}"
                )
            }
            DataResult.Error(t)
        }
    }

    override suspend fun kickMembers(teamspaceId: String, memberIds: List<String>): DataResult<Unit> {
        return try {
            for (id in memberIds) {
                val r = teamspaceApi.deleteMember(teamspaceId, id)
                if (!r.success) return DataResult.Error(Exception(r.message ?: "멤버 내보내기 실패"))
            }
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }
}
